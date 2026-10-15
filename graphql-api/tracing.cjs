if(process.env.OTEL_EXPORTER_OTLP_ENDPOINT) {
  const {NodeSDK}=require('@opentelemetry/sdk-node');
  const {getNodeAutoInstrumentations}=require('@opentelemetry/auto-instrumentations-node');
  const {OTLPTraceExporter}=require('@opentelemetry/exporter-trace-otlp-http');
  const sdk=new NodeSDK({traceExporter:new OTLPTraceExporter(),instrumentations:[getNodeAutoInstrumentations()]});
  sdk.start();process.on('SIGTERM',()=>sdk.shutdown().finally(()=>process.exit(0)));
}
