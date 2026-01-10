package mochineko.natural_disaster.api;

import com.google.gson.Gson;
import mochineko.natural_disaster.Main;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class P2PEarthquakeAPI extends DisasterAPI {

    private static BukkitTask task;

    private Earthquake earthquake;
    private Point[] points;
    private String time;

    public static P2PEarthquakeAPI[] getAPI() throws IOException {
        URL url = new URL("https://api.p2pquake.net/v2/history?codes=551");
        HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();

        String inputLine;
        while((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        con.disconnect();
        return  (new Gson()).fromJson(content.toString(), P2PEarthquakeAPI[].class);
    }

    public static void startMonitor() {
        int duration = 10;
        if (task == null) {
            Bukkit.getLogger().info("地震APIの監視を開始しました。");
            new BukkitRunnable() {
                private static LocalDateTime beforeGetTime = LocalDateTime.MIN.plusMinutes(1);
                @Override
                public void run() {
                    try {
                        P2PEarthquakeAPI latestAPI = getAPI()[0];
                        Point[] pointList = latestAPI.getPoints();
                        Earthquake earthquake = latestAPI.getEarthquake();
                        Earthquake.Hypocenter hypocenter = earthquake.getHypocenter();
                        LocalDateTime nowDate = LocalDateTime.now();
                        LocalDateTime earthquakeDate = latestAPI.getLocalDateTime();
                        Bukkit.getLogger().info(Math.abs(Duration.between(nowDate, earthquakeDate).toSeconds()) + "\n" +
                                beforeGetTime + "\n" + earthquakeDate +  "\n" + beforeGetTime.isBefore(earthquakeDate));
                        if (beforeGetTime.isBefore(earthquakeDate)) {
                            if (Math.abs(Duration.between(nowDate, earthquakeDate).toSeconds()) <= 60) {
                                Bukkit.broadcastMessage("震源名：" + hypocenter.getName() + "\n" +
                                        "マグニチュード: " + hypocenter.getMagnitude() + "\n" +
                                        "深さ：" + hypocenter.getDepth());
                                for (Point point : pointList) {
                                    Bukkit.broadcastMessage("=======" + "\n" +
                                            "観測点" +  point.getPref() + point.getAddr() + "\n" +
                                            "震度:" + point.getScale() + "\n" +
                                            "========");
                                }
                                beforeGetTime = earthquakeDate;
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.runTaskTimer(Main.getPlugin(Main.class), 0L, 20L*duration);
        }
    }

    public Earthquake getEarthquake() {
        return earthquake;
    }

    public Point[] getPoints() {
        return points;
    }

    public String getTime() {
        return time;
    }

    public LocalDateTime getLocalDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
        return LocalDateTime.parse(time, formatter);
    }

    public static class Earthquake {
        private String domesticTsunami;
        private String foreignTsunami;
        private Hypocenter hypocenter;
        private int maxScale;
        private String name;
        private String time;

        public String getDomesticTsunami() {
            return domesticTsunami;
        }

        public String getForeignTsunami() {
            return foreignTsunami;
        }

        public Hypocenter getHypocenter() {
            return hypocenter;
        }

        public int getMaxScale() {
            return maxScale;
        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }

        public LocalDateTime getDateTime() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            return LocalDateTime.parse(time, formatter);
        }

        public static class Hypocenter {
            private int depth;
            private float latitude;
            private float longitude;
            private float magnitude;
            private String name;

            public int getDepth() {
                return depth;
            }

            public float getLatitude() {
                return latitude;
            }

            public float getLongitude() {
                return longitude;
            }

            public float getMagnitude() {
                return magnitude;
            }

            public String getName() {
                return name;
            }
        }
    }

    public static class Point {
        private String addr; //観測点名
        private String pref; //都道府県
        private int scale; //震度

        /**
         * 都道府県を返す関数
         * @return 文字列で返す（例：石川県）
         */
        public String getPref() {
            return pref;
        }

        /**
         *  観測点名を取得する関数
         * @return 文字列で返す（例：輪島市鳳至町）
         */
        public String getAddr() {
            return addr;
        }

        /**
         * 震度を生のデータで返す関数
         * @return intで返す
         */
        public int getScale() {
            return scale;
        }

    }
}
