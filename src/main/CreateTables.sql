create table if not exists course
(
    id           varchar(20) not null
        constraint course_pk
            primary key,
    name         varchar(50) not null,
    credit       integer,
    class_hour   integer,
    is_pf        boolean,
    prerequisite varchar(100)
);

create table if not exists semester
(
    id         serial
        constraint semester_pk
            primary key,
    name       varchar(20) not null,
    begin_time date        not null,
    end_time   date        not null
);

create table if not exists section
(
    id             integer default nextval('course_section_id_seq'::regclass) not null
        constraint course_section_pk
            primary key,
    course_id      varchar(20)                                                not null
        constraint fk_course
            references course,
    semester_id    integer                                                    not null
        constraint fk_semester
            references semester,
    name           varchar(50)                                                not null,
    total_capacity integer,
    left_capacity  integer
);

create table if not exists instructor
(
    id         serial
        constraint instructor_pk
            primary key,
    first_name varchar(30) not null,
    last_name  varchar(50) not null
);

create table if not exists section_class
(
    id            integer default nextval('course_section_class_id_seq'::regclass) not null
        constraint course_section_class_pk
            primary key,
    section_id    integer                                                          not null
        constraint fk_section
            references section,
    instructor_id integer                                                          not null
        constraint fk_instructor
            references instructor,
    day_of_week   integer,
    class_begin   smallint,
    class_end     smallint,
    location      varchar(30),
    week_list     smallint[]
);

create table if not exists department
(
    id   serial
        constraint department_pk
            primary key,
    name varchar(30) not null
);

create unique index if not exists department_name_uindex
    on department (name);

create table if not exists major
(
    id            serial
        constraint major_pk
            primary key,
    name          varchar(30) not null,
    department_id integer     not null
        constraint fk_department
            references department
);

create table if not exists major_course
(
    major_id      integer     not null
        constraint fk_major
            references major,
    course_id     varchar(20) not null
        constraint fk_course
            references course,
    is_compulsory boolean,
    constraint pk_major_course
        primary key (major_id, course_id)
);

create table if not exists student
(
    id            serial
        constraint student_pk
            primary key,
    major_id      integer     not null
        constraint fk_major
            references major,
    first_name    varchar(30) not null,
    last_name     varchar(50) not null,
    enrolled_date date        not null
);

create table if not exists student_section
(
    student_id integer not null
        constraint fk_student
            references student,
    section_id integer not null
        constraint fk_section
            references section,
    mark       integer,
    is_passed  boolean,
    constraint pk_student_section
        primary key (student_id, section_id)
);


