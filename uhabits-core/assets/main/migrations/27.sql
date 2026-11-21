alter table habits add column is_bad_habit integer not null default 0;
alter table habits add column bad_habit_since integer;
