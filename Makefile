.PHONY: bench test run

# Fixed-seed benchmark → benchmarks/report.json + benchmarks/tiers.png
bench:
	./mvnw -q compile exec:java -Dexec.mainClass=com.harbinger.bench.Benchmark

# Backend tests + coverage
test:
	./mvnw verify

# Run the API + paced demo feeder
run:
	./mvnw spring-boot:run
