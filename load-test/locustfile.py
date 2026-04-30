"""
Load test for the IoT pipeline via device-stimulator-service.

Flow: Locust → POST /api/emit → stimulator → ingestor → processor → alert

Shapes:
  GradualRampShape  : linearly ramps users over 10 min then holds
  SuddenSpikeShape  : instant jump to max, hold, then drop
  (sawtooth)        : drive manually via Locust UI user count slider

Run locally:
  locust --host http://localhost:8080

Run in Kubernetes (master picks up LOCUST_HOST env var automatically via --host flag):
  set host to http://device-stimulator-service:8080
"""

import time
from locust import HttpUser, task, between, LoadTestShape


class IngestorUser(HttpUser):
    """
    Each virtual user calls GET /api/ingest?batchSize=N continuously.
    Ingestor generates random sensor data internally and forwards to processor.
    """
    wait_time = between(0.5, 1.5)

    def on_start(self):
        import os
        self.batch_size = os.getenv("BATCH_SIZE", "10")

    @task(10)
    def generate_and_ingest(self):
        self.client.get(
            f"/api/stimulate?batchSize={self.batch_size}",
            name="GET /api/stimulate",
        )

    @task(1)
    def status(self):
        self.client.get("/api/status", name="GET /api/status")


# ── Load shapes ───────────────────────────────────────────────────────────────

class GradualRampShape(LoadTestShape):
    """
    1× → 10× users linearly over 10 minutes, then holds for 5 minutes.
    Mirrors GradualRampTrafficStrategy timing.
    """
    min_users     = 5
    max_users     = 50
    ramp_duration = 10 * 60  # 10 min
    hold_duration = 5  * 60  # 5 min hold at peak
    spawn_rate    = 1

    def tick(self):
        run_time = self.get_run_time()
        if run_time > self.ramp_duration + self.hold_duration:
            return None

        if run_time <= self.ramp_duration:
            progress = run_time / self.ramp_duration
            users    = int(self.min_users + progress * (self.max_users - self.min_users))
        else:
            users = self.max_users

        return (users, self.spawn_rate)


class SuddenSpikeShape(LoadTestShape):
    """
    Baseline → instant spike → cooldown.
    Mirrors SuddenSpikeTrafficStrategy (20× batch).
    """
    baseline_users = 5
    spike_users    = 50
    spawn_rate     = 100     # as fast as possible
    warmup         = 30      # seconds at baseline
    spike_duration = 120     # seconds at spike
    cooldown       = 60      # seconds back at baseline

    def tick(self):
        run_time = self.get_run_time()
        total    = self.warmup + self.spike_duration + self.cooldown
        if run_time > total:
            return None

        if run_time < self.warmup:
            return (self.baseline_users, self.spawn_rate)
        elif run_time < self.warmup + self.spike_duration:
            return (self.spike_users, self.spawn_rate)
        else:
            return (self.baseline_users, self.spawn_rate)
