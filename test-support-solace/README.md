# test-support-solace
Reusable helpers to run Solace PubSub+ with Testcontainers.


### Quick start
1. Add this module to `settings.gradle` of your monorepo:
```gradle include(':test-support-solace')

2. In your service's build.gradle, depend on it:

testImplementation(project(':test-support-solace'))

3. In a test, start the container and wire your properties (example shown in SolaceTestSupport).

What you get

SolacePubSubContainer – thin wrapper over GenericContainer with sensible defaults.

SolaceAdmin – queue/topic provisioning via JCSMP session.provision.

SolaceClient – minimal publish/consume helpers for bytes/JSON.

SolaceTestSupport – convenience to bootstrap container & build a ready JCSMP session.

Await – tiny Awaitility wrappers for clean, readable waits.