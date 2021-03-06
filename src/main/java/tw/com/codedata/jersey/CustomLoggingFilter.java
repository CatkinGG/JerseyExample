package tw.com.codedata.jersey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.JSONP;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import test.Config;
import test.Report;
import test.RsSyncStatus;
import test.RsTransferRecords;
import test.TestCase;

/**
 * Servlet Filter implementation class CustomLoggingFilter
 */
public class CustomLoggingFilter extends LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
	
	private static int orderId = 0;
	private static int jobId = 0;
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		StringBuilder sb = new StringBuilder();
//		sb.append("User: ").append(requestContext.getSecurityContext().getUserPrincipal() == null ? "unknown"
//				: requestContext.getSecurityContext().getUserPrincipal());
		String path = requestContext.getUriInfo().getPath();
		sb.append("\n-Path:").append(path);
		sb.append("\n-Parameters:").append(getPathParameters(requestContext));
		sb.append("\n-Header:").append(requestContext.getHeaders());
		String body = getEntityBody(requestContext);
		sb.append("\n-Body:").append(body);
		//
		System.out.println("st========================================");
		System.out.println("HTTP REQUEST : " + sb.toString());

		if (Config.isRunTestCase) {
			if (path.toLowerCase().indexOf("sync_status") != -1) {
				Gson g = new GsonBuilder().create();
				RsSyncStatus rsSyncStatus = g.fromJson(body, RsSyncStatus.class);
				if (isLogoutFinish(rsSyncStatus))
					TestCase.putIdelCommand(orderId, jobId);
				else if (isReady(rsSyncStatus))
					TestCase.putNextCommand(rsSyncStatus, orderId, jobId);
				jobId++;
				orderId++;
			} else if (path.toLowerCase().indexOf("transfer_records") != -1) {
				calSuccessRate(body);
				TestCase.setCommand("");
			}
		}
	}

	private static int success = 0;
	private static int total = 0;

	private void calSuccessRate(String body) {
		Gson g = new GsonBuilder().create();
		RsTransferRecords rsTransferRecords = g.fromJson(body, RsTransferRecords.class);
		if (rsTransferRecords.getError_code().equals("03") || rsTransferRecords.getError_code().equals("04")) {
			System.out.println("************************************************************************** " + "End of Job - " + rsTransferRecords.getJob_id() + " **********************************************************************************");
				return;
		}
		total++;

		if (rsTransferRecords.getStatus().equals("0") 
				&& rsTransferRecords.getError_code().equals("") 
				&& !rsTransferRecords.getTransfer_code().equals("")
				&& !rsTransferRecords.getTransfer_code().equals("null")
				) {
			success++;
		}

		System.out.println("第" + total + "次轉帳。 (job id = " + rsTransferRecords.getJob_id()+ ")");
		String rate = new DecimalFormat("0.00").format((((float) success / (float) total) * 100));
		System.out.println("轉帳成功率: " + success + " / " + total + " = " + rate + "%");
		Report.transferCount++;
		Report.transferEndTimer = Calendar.getInstance();
		//
		long rsTime = Report.transferEndTimer.getTimeInMillis() - Report.transferStartTimer.getTimeInMillis();
		Report.totalTimer += rsTime;
		System.out.println("本次完成時間 = " + rsTime / 1000 + "秒");
		System.out.println("平均完成時間 = " + Report.totalTimer / Report.transferCount / 1000 + "秒");
		System.out.println("************************************************************************** " + "End of Job - " + rsTransferRecords.getJob_id() + " **********************************************************************************");
	}


	private boolean isLogoutFinish(RsSyncStatus rsSyncStatus) {

		return rsSyncStatus.getStatus_type().equals("0");
	}
	
private boolean isReady(RsSyncStatus rsSyncStatus) {

		return rsSyncStatus.getStatus_type().equals("3");
	}

	private String getPathParameters(ContainerRequestContext requestContext) {

		MultivaluedMap<String, String> map = requestContext.getUriInfo().getQueryParameters();
		List<String> keys = new ArrayList(map.keySet());
		List<String> strResult = new ArrayList();
		for (int i = 0; i < keys.size(); i++) {
			strResult.add(keys.get(i) + "=" + map.getFirst(keys.get(i)));
		}

		return strResult.toString();
	}

	private String getEntityBody(ContainerRequestContext requestContext) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = requestContext.getEntityStream();

		final StringBuilder b = new StringBuilder();
		try {
			ReaderWriter.writeTo(in, out);

			byte[] requestEntity = out.toByteArray();
			if (requestEntity.length == 0) {
				b.append("").append("\n");
			} else {
				b.append(new String(requestEntity)).append("\n");
			}
			requestContext.setEntityStream(new ByteArrayInputStream(requestEntity));

		} catch (IOException ex) {
			// Handle logging error
		}
		return b.toString();
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-Header:").append(responseContext.getHeaders());
		sb.append("\n-Body:").append(responseContext.getEntity());
		System.out.println("HTTP RESPONSE :" + sb.toString());
		System.out.println("end========================================");
	}
}
