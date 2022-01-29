create table course
(
    id           varchar(20) primary key,
    name         varchar(50) not null,
    credit       integer,
    class_hour   integer,
    is_pf        boolean,
    prerequisite varchar(100)
);

create table semester
(
    id         serial primary key,
    name       varchar(20) not null,
    begin_time date        not null,
    end_time   date        not null
);

create table section
(
    id             serial primary key,
    course_id      varchar(20) not null references course(id),
    semester_id    integer                                                    not null
        references semester,
    name           varchar(50)                                                not null,
    total_capacity integer,
    left_capacity  integer
);

create table instructor
(
    id        serial primary key,
    full_name varchar(50)
);

create table section_class
(
    id            serial primary key,
    section_id    integer                                                          not null
         references section,
    instructor_id integer                                                          not null
         references instructor,
    day_of_week   integer,
    class_begin   smallint,
    class_end     smallint,
    location      varchar(30),
    week_list     mediumint
);

create table department
(
    id   serial primary key,
    name varchar(30) not null
);

create unique index department_name_uindex
    on department (name);

create table major
(
    id            serial primary key,
    name          varchar(30) not null,
    department_id integer     not null
         references department
);

create table major_course
(
    major_id      integer     not null
          references major,
    course_id     varchar(20) not null
         references course,
    is_compulsory boolean,
    constraint pk_major_course
        primary key (major_id, course_id)
);

create table student
(
    id            serial
         primary key,
    major_id      integer not null
         references major,
    enrolled_date date    not null,
    full_name     varchar(50)
);

create table student_section
(
    student_id integer not null
         references student,
    section_id integer not null
         references section,
    mark       integer,
    constraint pk_student_section
        primary key (student_id, section_id)
);