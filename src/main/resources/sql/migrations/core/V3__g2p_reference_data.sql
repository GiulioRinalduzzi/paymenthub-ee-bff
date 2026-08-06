--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

-- G2P reference data, from the former ph-ee-operations-g2p-service. That
-- service let Hibernate create these tables at runtime with
-- spring.jpa.hibernate.ddl-auto=update; here they are versioned like every
-- other table in this application.
--
-- They live in the core schema, not in the tenant schemas: the operations web
-- console calls these endpoints without a Platform-TenantId header, so the
-- data is shared by all tenants (see TenantAwareHeaderFilter).

CREATE TABLE `Government_Entity` (
    `gov_inst_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`gov_inst_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `program` (
    `program_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `program_name` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`program_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `DFSP` (
    `fsp_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`fsp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `g2p_payment_config` (
    `config_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `account` VARCHAR(255) NOT NULL,
    `status` VARCHAR(255) NOT NULL,
    `gov_inst_id` BIGINT(20) DEFAULT NULL,
    `fsp_id` BIGINT(20) DEFAULT NULL,
    `program_id` BIGINT(20) DEFAULT NULL,
    PRIMARY KEY (`config_id`),
    CONSTRAINT `fk_g2p_payment_config_gov_inst` FOREIGN KEY (`gov_inst_id`) REFERENCES `Government_Entity` (`gov_inst_id`),
    CONSTRAINT `fk_g2p_payment_config_fsp` FOREIGN KEY (`fsp_id`) REFERENCES `DFSP` (`fsp_id`),
    CONSTRAINT `fk_g2p_payment_config_program` FOREIGN KEY (`program_id`) REFERENCES `program` (`program_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
