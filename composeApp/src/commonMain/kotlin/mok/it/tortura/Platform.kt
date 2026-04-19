package mok.it.tortura

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
