package com.fitbase.data.health;

import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.response.ReadRecordsResponse;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Puente Java→Kotlin para llamar la API suspend de Health Connect
 * desde un hilo de fondo. Usa Continuation + CountDownLatch.
 */
public class BlockingHealthConnect {

    @SuppressWarnings("unchecked")
    public static <T extends Record> List<T> readRecords(
            HealthConnectClient client,
            ReadRecordsRequest<T> request) {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<T>> resultRef = new AtomicReference<>(Collections.emptyList());

        try {
            Object result = client.readRecords(request, new Continuation<ReadRecordsResponse<T>>() {
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(Object o) {
                    try {
                        if (o instanceof ReadRecordsResponse) {
                            ReadRecordsResponse<T> resp = (ReadRecordsResponse<T>) o;
                            resultRef.set(resp.getRecords());
                        }
                    } catch (Exception ignored) {}
                    latch.countDown();
                }
            });

            // Si no suspendió (resultado inmediato)
            if (result instanceof ReadRecordsResponse) {
                return ((ReadRecordsResponse<T>) result).getRecords();
            }

            // Esperar resultado async (max 10s)
            latch.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            // HC no disponible, permisos denegados, etc.
            return Collections.emptyList();
        }

        return resultRef.get();
    }
}
