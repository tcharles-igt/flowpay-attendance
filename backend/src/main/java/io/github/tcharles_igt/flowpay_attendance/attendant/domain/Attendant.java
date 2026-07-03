package io.github.tcharles_igt.flowpay_attendance.attendant.domain;

import io.github.tcharles_igt.flowpay_attendance.shared.domain.BaseEntity;
import io.github.tcharles_igt.flowpay_attendance.shared.domain.TeamType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendants")
public class Attendant extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TeamType team;

	@Column(nullable = false)
	private boolean active = true;

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TeamType getTeam() {
		return team;
	}

	public void setTeam(TeamType team) {
		this.team = team;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
