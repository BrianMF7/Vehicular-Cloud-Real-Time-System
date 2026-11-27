-- Run in the team's existing schema. Only accepted submissions belong here.
USE `SWE Database`;

CREATE TABLE IF NOT EXISTS jobs (
    job_id VARCHAR(100) NOT NULL PRIMARY KEY,
    request_id CHAR(36) NOT NULL UNIQUE,
    client_id VARCHAR(100) NOT NULL,
    requester_name VARCHAR(255) NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    duration_hours INT NOT NULL,
    priority INT NOT NULL,
    availability_date VARCHAR(32) NOT NULL,
    availability_time VARCHAR(32) NOT NULL,
    deadline_date VARCHAR(32) NOT NULL,
    deadline_time VARCHAR(32) NOT NULL,
    submitted_at_millis BIGINT NOT NULL,
    accepted_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_job_duration CHECK (duration_hours > 0),
    CONSTRAINT chk_job_priority CHECK (priority > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id VARCHAR(100) NOT NULL PRIMARY KEY,
    request_id CHAR(36) NOT NULL UNIQUE,
    owner_id VARCHAR(255) NOT NULL,
    requester_name VARCHAR(255) NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    model_year SMALLINT NOT NULL,
    arrival_time VARCHAR(32) NOT NULL,
    departure_time VARCHAR(32) NOT NULL,
    compute_power DOUBLE NOT NULL,
    submitted_at_millis BIGINT NOT NULL,
    accepted_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_vehicle_compute CHECK (compute_power >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
