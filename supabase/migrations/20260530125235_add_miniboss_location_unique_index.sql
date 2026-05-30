create unique index "Tasks_one_miniboss_per_location"
on "public"."Tasks" ("locationId")
where "isMiniBoss" is true;
