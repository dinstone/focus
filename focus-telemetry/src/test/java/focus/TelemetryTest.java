/*
 * Copyright (C) 2019~2023 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package focus;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

public class TelemetryTest {

	private static Tracer tracer;

	public static void main(String[] args) {
		OpenTelemetry openTelemetry = getOpenTelemetry();

		tracer = openTelemetry.getTracer("telemetry-test-service", "1.0.0");

		Span span = tracer.spanBuilder("main").startSpan();
		// Make the span the current span
		try (Scope ss = span.makeCurrent()) {
			// In this scope, the span is the current/active span
			span.addEvent("parentTwo start");
			parentTwo();
			span.addEvent("parentTwo end");
		} finally {
			span.end();
		}

		System.out.println("over");
	}

	static void parentTwo() {
		Span parentSpan = tracer.spanBuilder("parent").startSpan();
		try (Scope scope = parentSpan.makeCurrent()) {
			parentSpan.setAttribute("local", true);
			parentSpan.setAttribute("method", "childTwo");

			childTwo();
		} finally {
			parentSpan.end();
		}
	}

	static void childTwo() {
		Span childSpan = tracer.spanBuilder("child").startSpan();
		try (Scope scope = childSpan.makeCurrent()) {
			// do stuff
		} finally {
			childSpan.end();
		}
	}

	private static OpenTelemetry getOpenTelemetry() {
		Resource resource = Resource.getDefault()
				.merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "logical-service-name")));

		SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
				.addSpanProcessor(BatchSpanProcessor.builder(LoggingSpanExporter.create()).build())
				.setResource(resource).build();

		OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
				.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
				.buildAndRegisterGlobal();
		return openTelemetry;
	}

}
