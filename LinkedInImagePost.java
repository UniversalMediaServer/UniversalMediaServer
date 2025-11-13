import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class LinkedInImagePost {
    private static final String ACCESS_TOKEN = "YOUR_LINKEDIN_ACCESS_TOKEN";
    private static final String AUTHOR_URN = "urn:li:person:YOUR_PROFILE_ID";

    public static void main(String[] args) throws Exception {
        String uploadUrl = initializeImageUpload();
        uploadImage(uploadUrl, "C:/path/to/your/image.jpg");
        createLinkedInPost();
    }

    private static String initializeImageUpload() throws Exception {
        String apiUrl = "https://api.linkedin.com/v2/assets?action=registerUpload";
        String jsonInput = "{"
                + "\"registerUploadRequest\": {"
                + "    \"owner\": \"" + AUTHOR_URN + "\","
                + "    \"recipes\": [\"urn:li:digitalmediaRecipe:feedshare-image\"],"
                + "    \"serviceRelationships\": [{"
                + "        \"identifier\": \"urn:li:userGeneratedContent\","
                + "        \"relationshipType\": \"OWNER\""
                + "    }]"
                + "}}";

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonInput.getBytes());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();
        System.out.println("Upload Init Response: " + response);
        return response.toString();
    }

    private static void uploadImage(String uploadUrl, String imagePath) throws Exception {
        File file = new File(imagePath);
        HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);
        conn.setRequestProperty("Content-Type", "image/jpeg");
        conn.setDoOutput(true);

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = conn.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("Image uploaded with response: " + conn.getResponseCode());
    }

    private static void createLinkedInPost() throws Exception {
        String apiUrl = "https://api.linkedin.com/v2/ugcPosts";
        String jsonInput = "{"
                + "\"author\": \"" + AUTHOR_URN + "\","
                + "\"lifecycleState\": \"PUBLISHED\","
                + "\"specificContent\": {"
                + "  \"com.linkedin.ugc.ShareContent\": {"
                + "    \"shareCommentary\": {\"text\": \"Posting image from Java 🚀\"},"
                + "    \"shareMediaCategory\": \"IMAGE\""
                + "  }"
                + "},"
                + "\"visibility\": {\"com.linkedin.ugc.MemberNetworkVisibility\": \"PUBLIC\"}"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonInput.getBytes());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();
        System.out.println("Post Response: " + response);
    }
}
