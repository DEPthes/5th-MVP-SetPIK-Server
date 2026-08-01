package com.setpik.server.performance.domain;

import com.setpik.server.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Flyway의 Venues 테이블을 그대로 매핑한다. */
@Entity
@Table(name = "Venues")
public class Venue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "venue_id", nullable = false)
	private Long venueId;

	@Column(name = "kopis_venue_id", nullable = true, length = 255)
	private String kopisVenueId;

	@Column(name = "venue_name", nullable = false, length = 255)
	private String venueName;

	@Column(name = "city", nullable = false, length = 255)
	private String city;

	@Column(name = "district", nullable = true, length = 255)
	private String district;

	@Column(name = "address", nullable = true, length = 255)
	private String address;

	@Column(name = "latitude", nullable = true, precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(name = "longitude", nullable = true, precision = 10, scale = 7)
	private BigDecimal longitude;

	protected Venue() {
	}

	public Long getVenueId() {
		return venueId;
	}

	public String getKopisVenueId() {
		return kopisVenueId;
	}

	public String getVenueName() {
		return venueName;
	}

	public String getCity() {
		return city;
	}

	public String getDistrict() {
		return district;
	}

	public String getAddress() {
		return address;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

}
