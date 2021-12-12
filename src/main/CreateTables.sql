create table course
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

create table semester
(
    id         serial
        constraint semester_pk
            primary key,
    name       varchar(20) not null,
    begin_time date        not null,
    end_time   date        not null
);

create table section
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

create table instructor
(
    id        serial
        constraint instructor_pk
            primary key,
    full_name varchar(50)
);

create table section_class
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

create table department
(
    id   serial
        constraint department_pk
            primary key,
    name varchar(30) not null
);

create unique index department_name_uindex
    on department (name);

create table major
(
    id            serial
        constraint major_pk
            primary key,
    name          varchar(30) not null,
    department_id integer     not null
        constraint fk_department
            references department
);

create table major_course
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

create table student
(
    id            serial
        constraint student_pk
            primary key,
    major_id      integer not null
        constraint fk_major
            references major,
    enrolled_date date    not null,
    full_name     varchar(50)
);

create table student_section
(
    student_id integer not null
        constraint fk_student
            references student,
    section_id integer not null
        constraint fk_section
            references section,
    mark       integer,
    constraint pk_student_section
        primary key (student_id, section_id)
);


