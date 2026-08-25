package com.setpik.server.kopis.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import org.junit.jupiter.api.Test;

class KopisXmlParserTest {

	private final KopisXmlParser parser = new KopisXmlParser();

	@Test
	void parsesPerformanceListIds() {
		String xml = """
			<dbs>
			  <db><mt20id>PF001</mt20id></db>
			  <db><mt20id>PF002</mt20id></db>
			</dbs>
			""";

		assertThat(parser.performanceIds(xml)).containsExactly("PF001", "PF002");
	}

	@Test
	void mapsKopisDetailToErdSourceFields() {
		String xml = """
			<dbs><db>
			  <mt20id>PF001</mt20id>
			  <prfnm>SetPIK Festival</prfnm>
			  <prfpdfrom>2026.08.15</prfpdfrom>
			  <prfpdto>2026.08.17</prfpdto>
			  <poster>https://example.com/poster.jpg</poster>
			  <prfstate>공연예정</prfstate>
			  <pcseguidance>전석 100,000원</pcseguidance>
			  <prfruntime>180분</prfruntime>
			  <prfage>만 12세 이상</prfage>
			  <area>인천광역시</area>
			  <genrenm>대중음악</genrenm>
			  <mt10id>FC001</mt10id>
			  <fcltynm>송도달빛축제공원</fcltynm>
			  <prfcast>Artist A, Artist B</prfcast>
			  <festival>Y</festival>
			  <visit>Y</visit>
			  <relates><relate><relateurl>https://tickets.example.com/1</relateurl></relate></relates>
			</db></dbs>
			""";

		KopisPerformanceDetail detail = parser.performanceDetail(xml);

		assertThat(detail.kopisPerformanceId()).isEqualTo("PF001");
		assertThat(detail.startDate()).isEqualTo(LocalDate.of(2026, 8, 15));
		assertThat(detail.facilityId()).isEqualTo("FC001");
		assertThat(detail.artistNames()).containsExactly("Artist A", "Artist B");
		assertThat(detail.bookingUrl()).isEqualTo("https://tickets.example.com/1");
		assertThat(detail.priceText()).isEqualTo("전석 100,000원");
		assertThat(detail.runningTime()).isEqualTo("180분");
		assertThat(detail.ageRestriction()).isEqualTo("만 12세 이상");
		assertThat(detail.festival()).isTrue();
		assertThat(detail.international()).isTrue();
	}

	@Test
	void parsesVenueDetail() {
		String xml = """
			<dbs><db>
			  <mt10id>FC001</mt10id>
			  <fcltynm>송도달빛축제공원</fcltynm>
			  <adres>인천광역시 연수구 센트럴로 350</adres>
			  <la>37.3921</la>
			  <lo>126.6399</lo>
			</db></dbs>
			""";

		KopisVenueDetail venue = parser.venueDetail(xml);

		assertThat(venue.facilityId()).isEqualTo("FC001");
		assertThat(venue.latitude()).isEqualByComparingTo(new BigDecimal("37.3921"));
		assertThat(venue.longitude()).isEqualByComparingTo(new BigDecimal("126.6399"));
	}
}
