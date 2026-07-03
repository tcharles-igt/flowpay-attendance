alter table attendants
    add constraint chk_attendants_team
        check (team in ('CARDS', 'LOANS', 'OTHERS'));

alter table attendances
    add constraint chk_attendances_subject
        check (subject in ('CARD_PROBLEM', 'LOAN_REQUEST', 'OTHER'));

alter table attendances
    add constraint chk_attendances_team
        check (team in ('CARDS', 'LOANS', 'OTHERS'));

alter table attendances
    add constraint chk_attendances_status
        check (status in ('WAITING', 'IN_PROGRESS', 'FINISHED'));

alter table attendances
    add constraint chk_attendances_finished_requires_started
        check (finished_at is null or started_at is not null);

alter table attendances
    add constraint chk_attendances_finished_after_started
        check (finished_at is null or finished_at >= started_at);
