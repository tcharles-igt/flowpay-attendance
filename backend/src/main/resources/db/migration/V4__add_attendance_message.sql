alter table attendances add column message varchar(500);

update attendances
set message = 'Solicitacao registrada sem mensagem detalhada.'
where message is null;

alter table attendances alter column message set not null;
