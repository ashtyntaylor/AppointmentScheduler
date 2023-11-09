import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;

public class HttpHelpers {
    public static CloseableHttpResponse doGet(String uri) throws IOException, ParseException {
        // Create client
        CloseableHttpClient client = HttpClients.createDefault();

        try {
            // Process request
            final HttpGet request = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(request);

            return response;
        } finally {
            client.close();
        }


    }

    public static CloseableHttpResponse doPost(String uri, String requestBody) throws IOException, ParseException {
        // Create client
        final CloseableHttpClient client = HttpClients.createDefault();

        try {
            // Process request
            HttpPost request = new HttpPost(uri);

            if (requestBody != null) {
                StringEntity entity = new StringEntity(requestBody);
                request.setEntity(entity);
            }

            CloseableHttpResponse response = client.execute(request);

            return response;
        } finally {
            client.close();
        }
    }
}
