-- Dummy data for local development and manual testing.
-- Supabase runs this file after migrations during `supabase db reset`.
-- Seed IDs intentionally use a high range to avoid colliding with hand-created test data.

insert into "public"."Games" ("id", "name")
values
  (900001, 'Demo Game - Logic Castle'),
  (900002, 'Demo Game - Forest Trial')
on conflict ("id") do update
set "name" = excluded."name";

insert into "public"."Locations" ("id", "name", "gameId")
values
  (900001, 'Great Hall', 900001),
  (900002, 'Library', 900001),
  (900003, 'Potion Room', 900001),
  (900004, 'Courtyard', 900001),
  (900005, 'North Gate', 900002),
  (900006, 'Old Mill', 900002)
on conflict ("id") do update
set
  "name" = excluded."name",
  "gameId" = excluded."gameId";

insert into "public"."Tasks" ("id", "text", "solution", "isMiniBoss", "gameId", "locationId")
values
  (900001, 'What number comes next: 2, 4, 8, 16, ?', '32', false, 900001, 900001),
  (900002, 'Solve: 12 + 7 - 5', '14', false, 900001, 900001),
  (900003, 'Which word is the odd one out: circle, square, triangle, apple?', 'apple', false, 900001, 900002),
  (900004, 'Decode the pattern: A=1, B=2, C=3. What is DOG?', '4-15-7', false, 900001, 900002),
  (900005, 'Mini boss: If every blue key opens two doors, how many doors do seven keys open?', '14', true, 900001, 900003),
  (900006, 'Balance the equation: 3 + ? = 11', '8', false, 900001, 900004),
  (900007, 'Find the missing number: 5, 10, 20, ?', '40', false, 900002, 900005),
  (900008, 'Mini boss: A team has 24 points and wins 3 tasks worth 6 points each. Total?', '42', true, 900002, 900006)
on conflict ("id") do update
set
  "text" = excluded."text",
  "solution" = excluded."solution",
  "isMiniBoss" = excluded."isMiniBoss",
  "gameId" = excluded."gameId",
  "locationId" = excluded."locationId";

insert into "public"."HealingTasks" ("id", "text", "solution", "gameId")
values
  (900001, 'Healing: Name a prime number greater than 10 and less than 20.', '11', 900001),
  (900002, 'Healing: Reverse the word LOGIC.', 'CIGOL', 900001),
  (900003, 'Healing: Solve 9 x 6.', '54', 900001),
  (900004, 'Healing: What is half of 88?', '44', 900002)
on conflict ("id") do update
set
  "text" = excluded."text",
  "solution" = excluded."solution",
  "gameId" = excluded."gameId";

insert into "public"."TeamAssignment" ("id", "baseTeamCounter", "gameId")
values
  (900001, 3, 900001),
  (900002, 2, 900002)
on conflict ("id") do update
set
  "baseTeamCounter" = excluded."baseTeamCounter",
  "gameId" = excluded."gameId";

insert into "public"."Teams" ("id", "name", "teamAssignmentId")
values
  (900001, 'Red Logic', 900001),
  (900002, 'Blue Cipher', 900001),
  (900003, 'Green Matrix', 900001),
  (900004, 'Silver Signal', 900002),
  (900005, 'Gold Vector', 900002)
on conflict ("id") do update
set
  "name" = excluded."name",
  "teamAssignmentId" = excluded."teamAssignmentId";

insert into "public"."Students" ("id", "name", "group", "klass", "teamId")
values
  (900001, 'Anna Kovacs', 'A', '7', 900001),
  (900002, 'Bence Toth', 'A', '7', 900001),
  (900003, 'Csenge Nagy', 'B', '7', 900002),
  (900004, 'Daniel Szabo', 'B', '7', 900002),
  (900005, 'Emma Varga', 'C', '8', 900003),
  (900006, 'Ferenc Kiss', 'C', '8', 900003),
  (900007, 'Greta Farkas', 'A', '8', 900004),
  (900008, 'Hanna Balogh', 'B', '8', 900005)
on conflict ("id") do update
set
  "name" = excluded."name",
  "group" = excluded."group",
  "klass" = excluded."klass",
  "teamId" = excluded."teamId";

insert into "public"."TasksLedger" ("id", "taskId", "teamId", "userId", "isSuccess")
values
  (900001, 900001, 900001, 101, true),
  (900002, 900002, 900001, 101, false),
  (900009, 900003, 900001, 101, false),
  (900010, 900005, 900001, 101, false),
  (900003, 900003, 900002, 102, true),
  (900004, 900004, 900002, 102, false),
  (900011, 900001, 900002, 102, false),
  (900005, 900005, 900003, 103, true),
  (900006, 900006, 900003, 103, false),
  (900012, 900002, 900003, 103, false),
  (900007, 900007, 900004, 104, true),
  (900013, 900008, 900004, 104, false),
  (900008, 900008, 900005, 105, false),
  (900014, 900007, 900005, 105, false)
on conflict ("id") do update
set
  "taskId" = excluded."taskId",
  "teamId" = excluded."teamId",
  "userId" = excluded."userId",
  "isSuccess" = excluded."isSuccess";

insert into "public"."HealingLedger" ("id", "teamId", "healingTaskId", "healedTasksLedgerId", "userId")
values
  (900001, 900001, 900001, 900002, 101),
  (900002, 900002, 900002, 900004, 102),
  (900003, 900003, 900003, 900006, 103),
  (900004, 900005, 900004, 900008, 105)
on conflict ("id") do update
set
  "teamId" = excluded."teamId",
  "healingTaskId" = excluded."healingTaskId",
  "healedTasksLedgerId" = excluded."healedTasksLedgerId",
  "userId" = excluded."userId";

insert into "public"."ItemEffect" ("id", "description")
values
  (900001, 'Double points for one location'),
  (900002, 'Double points for one team area'),
  (900003, 'Reveal one failed answer for review')
on conflict ("id") do update
set "description" = excluded."description";

insert into "public"."Item" ("id", "name", "price", "itemEffectId")
values
  (900001, 'Location Multiplier', 5, 900001),
  (900002, 'Area Multiplier', 8, 900002),
  (900003, 'Hint Scroll', 3, 900003)
on conflict ("id") do update
set
  "name" = excluded."name",
  "price" = excluded."price",
  "itemEffectId" = excluded."itemEffectId";

insert into "public"."Shop" ("id", "itemId", "targetId", "userId")
values
  (900001, 900001, 900001, 101),
  (900002, 900002, 900002, 102),
  (900003, 900003, 900005, 103)
on conflict ("id") do update
set
  "itemId" = excluded."itemId",
  "targetId" = excluded."targetId",
  "userId" = excluded."userId";

select setval('"public"."Games_id_seq"', greatest((select max("id") from "public"."Games"), 1), true);
select setval('"public"."Locations_id_seq"', greatest((select max("id") from "public"."Locations"), 1), true);
select setval('"public"."Tasks_id_seq"', greatest((select max("id") from "public"."Tasks"), 1), true);
select setval('"public"."HealingTasks_id_seq"', greatest((select max("id") from "public"."HealingTasks"), 1), true);
select setval('"public"."TeamAssignment_id_seq"', greatest((select max("id") from "public"."TeamAssignment"), 1), true);
select setval('"public"."Teams_id_seq"', greatest((select max("id") from "public"."Teams"), 1), true);
select setval('"public"."Students_id_seq"', greatest((select max("id") from "public"."Students"), 1), true);
select setval('"public"."TasksLedger_id_seq"', greatest((select max("id") from "public"."TasksLedger"), 1), true);
select setval('"public"."HealingLedger_id_seq"', greatest((select max("id") from "public"."HealingLedger"), 1), true);
select setval('"public"."ItemEffect_id_seq"', greatest((select max("id") from "public"."ItemEffect"), 1), true);
select setval('"public"."Item_id_seq"', greatest((select max("id") from "public"."Item"), 1), true);
select setval('"public"."Shop_id_seq"', greatest((select max("id") from "public"."Shop"), 1), true);
