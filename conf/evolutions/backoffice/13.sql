create table business_user_application_primary_addresses
(
	id             int unsigned auto_increment     primary key,
	uuid           varchar(36)                        not null,
	application_id int unsigned                       not null,
	address_type   varchar(30)                        not null,
	country_id     int unsigned                       not null,
	city           varchar(50)                        not null,
	postal_code    varchar(10)                        null,
	address        varchar(500)                       null,
	coordinate_x   decimal(8,5)                       null,
	coordinate_y   decimal(8,5)                       null,
	created_by     varchar(50)                        not null,
	created_at     datetime default CURRENT_TIMESTAMP null,
	updated_by     varchar(50)                        null,
	updated_at     datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
	
	foreign key (application_id) references business_user_applications (id),
	foreign key (country_id) references countries (id)
);

# --- !Downs

DROP TABLE `business_user_application_primary_addresses`;