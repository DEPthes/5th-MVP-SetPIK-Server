package com.setpik.server.kopis.client;

import com.setpik.server.common.exception.BusinessException;
import com.setpik.server.common.exception.ErrorCode;
import com.setpik.server.kopis.dto.KopisPerformanceDetail;
import com.setpik.server.kopis.dto.KopisVenueDetail;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class KopisXmlParser {

	private static final DateTimeFormatter KOPIS_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	public List<String> performanceIds(String xml) {
		Document document = document(xml);
		NodeList rows = document.getElementsByTagName("db");
		List<String> ids = new ArrayList<>();
		for (int index = 0; index < rows.getLength(); index++) {
			String id = text((Element) rows.item(index), "mt20id");
			if (!id.isBlank()) {
				ids.add(id);
			}
		}
		return ids;
	}

	public KopisPerformanceDetail performanceDetail(String xml) {
		Element row = firstRow(document(xml));
		return new KopisPerformanceDetail(
			text(row, "mt20id"),
			text(row, "prfnm"),
			date(text(row, "prfpdfrom")),
			date(text(row, "prfpdto")),
			nullIfBlank(text(row, "poster")),
			firstBookingUrl(row),
			text(row, "prfstate"),
			nullIfBlank(text(row, "pcseguidance")),
			nullIfBlank(text(row, "prfruntime")),
			nullIfBlank(text(row, "prfage")),
			text(row, "area"),
			text(row, "genrenm"),
			text(row, "mt10id"),
			text(row, "fcltynm"),
			artistNames(text(row, "prfcast"))
		);
	}

	public KopisVenueDetail venueDetail(String xml) {
		Element row = firstRow(document(xml));
		return new KopisVenueDetail(
			text(row, "mt10id"),
			text(row, "fcltynm"),
			nullIfBlank(text(row, "adres")),
			decimal(text(row, "la")),
			decimal(text(row, "lo"))
		);
	}

	private Document document(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private Element firstRow(Document document) {
		NodeList rows = document.getElementsByTagName("db");
		if (rows.getLength() == 0) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		return (Element) rows.item(0);
	}

	private String firstBookingUrl(Element row) {
		NodeList urls = row.getElementsByTagName("relateurl");
		return urls.getLength() == 0 ? null : nullIfBlank(urls.item(0).getTextContent().trim());
	}

	private List<String> artistNames(String cast) {
		if (cast.isBlank() || cast.equals("-")) {
			return List.of();
		}
		return Arrays.stream(cast.split("[,，/]"))
			.map(String::trim)
			.filter(name -> !name.isBlank())
			.distinct()
			.toList();
	}

	private String text(Element element, String tagName) {
		NodeList nodes = element.getElementsByTagName(tagName);
		return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
	}

	private LocalDate date(String value) {
		try {
			return LocalDate.parse(value, KOPIS_DATE);
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private BigDecimal decimal(String value) {
		try {
			return value.isBlank() ? null : new BigDecimal(value);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private String nullIfBlank(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
