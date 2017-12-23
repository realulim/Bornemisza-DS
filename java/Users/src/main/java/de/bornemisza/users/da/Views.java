package de.bornemisza.users.da;

public class Views {

    public static String getUuidSumByColor(String userDb) {
        return  "        {\n" +
                "          \"_id\": \"_design/" + userDb + "\",\n" +
                "          \"views\": {\n" +
                "            \"uuid_sum_by_color\": {\n" +
                "              \"map\": \"function(doc) { if (typeof doc.type !== 'undefined' && doc.type === 'uuid') { emit(doc.color, 1) } }\",\n" +
                "              \"reduce\": \"function(key, values, rereduce) { return sum(values) }\"\n" +
                "            }\n" +
                "          },\n" +
                "          \"lists\": {},\n" +
                "          \"shows\": {},\n" +
                "          \"language\": \"javascript\",\n" +
                "          \"filters\": {},\n" +
                "          \"updates\": {}\n" +
                "        }\n";
    }
    
}
