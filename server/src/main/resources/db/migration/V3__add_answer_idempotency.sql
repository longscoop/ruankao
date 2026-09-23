alter table answer_record
    add column idempotency_key varchar(128);

create unique index uq_answer_record_user_idempotency
    on answer_record(user_id, idempotency_key)
    where idempotency_key is not null;
