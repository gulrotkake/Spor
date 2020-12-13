package io.tightloop.spor;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DistanceUtil {
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private static final long EARTH_RADIUS = 6_378_136L;

    private static class E implements AutoCloseable {
        private final XmlSerializer xml;
        private final String namespace;
        private final String name;

        E(XmlSerializer xml, String name) throws IOException {
            this(xml, "", name);
        }

        E(XmlSerializer xml, String namespace, String name) throws IOException {
            this.xml = xml;
            this.namespace = namespace;
            this.name = name;
            xml.startTag(namespace, name);
        }

        @Override
        public void close() throws IOException {
            xml.endTag(namespace, name);
        }

        public E attr(String tag, String value) throws IOException {
            xml.attribute("", tag, value);
            return this;
        }

        public E attr(String tag, double value) throws IOException {
            xml.attribute("", tag, String.format(Locale.US, "%f", value));
            return this;
        }
    }

    private DistanceUtil() {
    }

    public static double distanceInMeters(double lat1, double lat2, double lng1, double lng2, double alt1, double alt2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = Math.pow(EARTH_RADIUS * c, 2) + Math.pow(alt1 - alt2, 2);

        return Math.sqrt(distance);
    }

    public static void spor2Gpx(File sporFile, File gpxFile) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(sporFile));
             FileOutputStream fos = new FileOutputStream(gpxFile)) {

            XmlSerializer xml = Xml.newSerializer();
            xml.setOutput(fos, StandardCharsets.UTF_8.name());
            xml.startDocument(StandardCharsets.UTF_8.name(), true);
            try (E ignored0 = new E(xml, "http://www.topografix.com/GPX/1/0", "gpx").attr("version", "1.0")
                    .attr("creator", "spor2gpx"); E ignored1 = new E(xml, "trkseg")) {
                while (dis.available() >= 24) {
                    double lat = dis.readDouble();
                    double lng = dis.readDouble();
                    double alt = dis.readDouble();
                    try (E ignored2 = new E(xml, "trkpt").attr("lat", lat).attr("lon", lng)) {
                        try (E ignored3 = new E(xml, "ele")) {
                            xml.text(String.format(Locale.US, "%f", alt));
                        }

                        try (E ignored4 = new E(xml, "time")) {
                            xml.text(DATE_FMT.format(new Date(dis.readLong())));
                        }
                    }
                }
            } finally {
                xml.endDocument();
                xml.flush();
            }
        }
    }
}
