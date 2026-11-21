
package hu.forex.app.service;

import hu.forex.app.model.ExchangeRate;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MnbSoapService {

    private static final String ENDPOINT =
            "https://www.mnb.hu/arfolyamok.asmx";

    private static final String NAMESPACE =
            "http://www.mnb.hu/webservices/";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<ExchangeRate> getExchangeRates(
            LocalDate startDate,
            LocalDate endDate,
            String currency) {

        if (startDate == null || endDate == null
                || currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Minden mező kitöltése kötelező.");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "A kezdő dátum nem lehet későbbi a befejező dátumnál.");
        }

        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Érvénytelen devizakód.");
        }

        String body = """
                <GetExchangeRates xmlns="%s">
                    <startDate>%s</startDate>
                    <endDate>%s</endDate>
                    <currencyNames>%s</currencyNames>
                </GetExchangeRates>
                """.formatted(
                NAMESPACE,
                startDate,
                endDate,
                currency
        );

        String response = callSoap(body);

        String result = extractResult(
                response,
                "GetExchangeRatesResult"
        );

        return parseRates(result);
    }

    private String callSoap(String operationBody) {

        String envelope = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                    xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">

                    <soap:Body>
                        %s
                    </soap:Body>

                </soap:Envelope>
                """.formatted(operationBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header(
                        "Content-Type",
                        "text/xml; charset=utf-8"
                )
                .header(
                        "SOAPAction",
                        "\"" + NAMESPACE
                                + "MNBArfolyamServiceSoap/GetExchangeRates\""
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                envelope,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "MNB HTTP hiba: " + response.statusCode()
                );
            }

            return response.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Az MNB lekérdezés megszakadt.", e
            );

        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            throw new IllegalStateException(
                    "Nem sikerült kapcsolódni az MNB szolgáltatásához.",
                    e
            );
        }
    }

    private String extractResult(
            String soapXml,
            String resultElementName) {

        try {
            Document document = parseXml(soapXml);

            NodeList nodes =
                    document.getElementsByTagNameNS(
                            "*",
                            resultElementName
                    );

            if (nodes.getLength() == 0) {
                throw new IllegalStateException(
                        "Az MNB válaszában nem található eredmény."
                );
            }

            return nodes.item(0).getTextContent();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Az MNB SOAP válasza nem értelmezhető.", e
            );
        }
    }

    private List<ExchangeRate> parseRates(String ratesXml) {

        List<ExchangeRate> rates = new ArrayList<>();

        if (ratesXml == null || ratesXml.isBlank()) {
            return rates;
        }

        try {
            Document document = parseXml(ratesXml);

            NodeList days =
                    document.getElementsByTagName("Day");

            for (int i = 0; i < days.getLength(); i++) {

                Element day = (Element) days.item(i);

                LocalDate date = LocalDate.parse(
                        day.getAttribute("date")
                );

                NodeList children = day.getChildNodes();

                for (int j = 0; j < children.getLength(); j++) {

                    Node node = children.item(j);

                    if (node instanceof Element rateElement
                            && "Rate".equals(rateElement.getTagName())) {

                        String currency =
                                rateElement.getAttribute("curr");

                        int unit = Integer.parseInt(
                                rateElement.getAttribute("unit")
                        );

                        BigDecimal rate = new BigDecimal(
                                rateElement.getTextContent()
                                        .trim()
                                        .replace(',', '.')
                        );

                        rates.add(
                                new ExchangeRate(
                                        date,
                                        currency,
                                        unit,
                                        rate
                                )
                        );
                    }
                }
            }

            return rates;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Az árfolyam XML feldolgozása nem sikerült.",
                    e
            );
        }
    }

    private Document parseXml(String xml) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        return factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(
                        xml.getBytes(StandardCharsets.UTF_8)
                )
        );
    }
}
