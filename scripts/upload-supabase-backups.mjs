import {createClient} from "@supabase/supabase-js"
import {createHash} from "node:crypto"
import {promises as fs} from "node:fs"
import path from "node:path"

const {SUPABASE_PROJECT_URL, SUPABASE_SECRET_KEY, BACKUP_TIMESTAMP} = process.env
const SUPABASE_BACKUP_BUCKET = process.env.SUPABASE_BACKUP_BUCKET?.trim() || "db-backups"
const SUPABASE_BACKUP_DIR = process.env.SUPABASE_BACKUP_DIR?.trim() || "backup-output"

if (!SUPABASE_PROJECT_URL) {
    throw new Error("SUPABASE_PROJECT_URL is required")
}

if (!SUPABASE_SECRET_KEY) {
    throw new Error("SUPABASE_SECRET_KEY is required")
}

if (!BACKUP_TIMESTAMP) {
    throw new Error("BACKUP_TIMESTAMP is required")
}

const supabase = createClient(SUPABASE_PROJECT_URL, SUPABASE_SECRET_KEY, {
    auth: {
        autoRefreshToken: false,
        persistSession: false,
    },
})

async function ensureBucket(bucketName) {
    const {data: buckets, error} = await supabase.storage.listBuckets()
    if (error) {
        throw new Error(`Failed to list storage buckets: ${error.message}`)
    }

    if (buckets.some((bucket) => bucket.id === bucketName || bucket.name === bucketName)) {
        return
    }

    const {error: createError} = await supabase.storage.createBucket(bucketName, {
        public: false,
        fileSizeLimit: "250MB",
        allowedMimeTypes: [
            "application/gzip",
            "application/json",
            "text/plain",
        ],
    })

    if (createError) {
        throw new Error(`Failed to create storage bucket '${bucketName}': ${createError.message}`)
    }
}

function sha256(buffer) {
    return createHash("sha256").update(buffer).digest("hex")
}

function contentTypeFor(fileName) {
    if (fileName.endsWith(".json")) {
        return "application/json"
    }

    if (fileName.endsWith(".gz")) {
        return "application/gzip"
    }

    return "text/plain"
}

async function uploadFile(bucketName, filePath, objectPath) {
    const fileBuffer = await fs.readFile(filePath)
    const {error} = await supabase.storage
        .from(bucketName)
        .upload(objectPath, fileBuffer, {
            contentType: contentTypeFor(filePath),
            upsert: false,
        })

    if (error) {
        throw new Error(`Failed to upload '${objectPath}': ${error.message}`)
    }

    return {
        bytes: fileBuffer.byteLength,
        file: path.basename(filePath),
        objectPath,
        sha256: sha256(fileBuffer),
    }
}

async function main() {
    await ensureBucket(SUPABASE_BACKUP_BUCKET)

    const entries = await fs.readdir(SUPABASE_BACKUP_DIR, {withFileTypes: true})
    const files = entries
        .filter((entry) => entry.isFile())
        .map((entry) => path.join(SUPABASE_BACKUP_DIR, entry.name))
        .sort()

    if (files.length === 0) {
        throw new Error(`No backup files found in '${SUPABASE_BACKUP_DIR}'`)
    }

    const basePrefix = `database/${BACKUP_TIMESTAMP}`
    const uploadedFiles = []

    for (const filePath of files) {
        uploadedFiles.push(
            await uploadFile(
                SUPABASE_BACKUP_BUCKET,
                filePath,
                `${basePrefix}/${path.basename(filePath)}`,
            ),
        )
    }

    const manifest = {
        bucket: SUPABASE_BACKUP_BUCKET,
        createdAt: new Date().toISOString(),
        files: uploadedFiles,
    }

    const manifestBuffer = Buffer.from(JSON.stringify(manifest, null, 2))
    const manifestPath = `${basePrefix}/manifest.json`
    const {error: manifestError} = await supabase.storage
        .from(SUPABASE_BACKUP_BUCKET)
        .upload(manifestPath, manifestBuffer, {
            contentType: "application/json",
            upsert: false,
        })

    if (manifestError) {
        throw new Error(`Failed to upload '${manifestPath}': ${manifestError.message}`)
    }

    console.log(
        JSON.stringify(
            {
                bucket: SUPABASE_BACKUP_BUCKET,
                files: uploadedFiles.length,
                manifestPath,
                prefix: basePrefix,
            },
            null,
            2,
        ),
    )
}

await main()
