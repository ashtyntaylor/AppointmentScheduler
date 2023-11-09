import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;

public class HttpHelpers {
    public static String doGet(String uri) throws IOException, ParseException, InvalidTokenException, SchedulerException {
        // Create client
        CloseableHttpClient client = HttpClients.createDefault();

        try {
            // Process request
            final HttpGet request = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(request);
            int status = response.getCode();
            String responseBody = null;

            if (response.getEntity() != null) {
                responseBody = EntityUtils.toString(response.getEntity());
            }

            if (status != 200 && status != 204) {
                throwAppropriateException(status, responseBody);
            }

            return responseBody;

        } finally {
            client.close();
        }
    }

    public static String doPost(String uri, String requestBody) throws IOException, ParseException, InvalidTokenException, SchedulerException {
        // Create client
        CloseableHttpClient client = HttpClients.createDefault();

        try {
            // Process request
            HttpPost request = new HttpPost(uri);

            if (requestBody != null) {
                StringEntity entity = new StringEntity(requestBody);
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json");
            }

            CloseableHttpResponse response = client.execute(request);
            int status = response.getCode();
            String responseBody = null;

            if (response.getEntity() != null) {
                responseBody = EntityUtils.toString(response.getEntity());
            }

            if (status != 200) {
                throwAppropriateException(status, responseBody);
            }

            return responseBody;

        } finally {
            client.close();
        }
    }

    private static void throwAppropriateException(int status, String message) throws SchedulerException, InvalidTokenException {
        if (status == 401) {
            throw new InvalidTokenException();
        }
        else if (status == 405) {
            throw new SchedulerException("The server has indicated an out of sequence call");
        }
        else if (status == 500) {
            throw new SchedulerException("Internal Server Error: " + message);
        }
        else {
            throw new SchedulerException("Unexpected status returned: " + status);
        }
    }
}
