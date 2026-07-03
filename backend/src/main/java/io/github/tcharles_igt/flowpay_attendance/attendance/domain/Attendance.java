package io.github.tcharles_igt.flowpay_attendance.attendance.domain;

import java.time.OffsetDateTime;

import io.github.tcharles_igt.flowpay_attendance.attendant.domain.Attendant;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.BaseEntity;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendances")
public class Attendance extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "customer_name", nullable = false, length = 150)
	private String customerName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AttendanceSubject subject;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TeamType team;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AttendanceStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "attendant_id")
	private Attendant attendant;

	@Column(name = "started_at")
	private OffsetDateTime startedAt;

	@Column(name = "finished_at")
	private OffsetDateTime finishedAt;

	public Long getId() {
		return id;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public AttendanceSubject getSubject() {
		return subject;
	}

	public void setSubject(AttendanceSubject subject) {
		this.subject = subject;
	}

	public TeamType getTeam() {
		return team;
	}

	public void setTeam(TeamType team) {
		this.team = team;
	}

	public AttendanceStatus getStatus() {
		return status;
	}

	public void setStatus(AttendanceStatus status) {
		this.status = status;
	}

	public Attendant getAttendant() {
		return attendant;
	}

	public void setAttendant(Attendant attendant) {
		this.attendant = attendant;
	}

	public OffsetDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(OffsetDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public OffsetDateTime getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(OffsetDateTime finishedAt) {
		this.finishedAt = finishedAt;
	}
}
