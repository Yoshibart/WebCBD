function fn() {
  // Use the Spring Boot random port when running integration tests.
  var port = karate.properties['karate.port'] || '8082';
  var baseUrl = 'http://localhost:' + port;

  return {
    baseUrl: baseUrl
  };
}
