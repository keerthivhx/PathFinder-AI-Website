/**
 * PathFinder AI Backend API Verification & Test Suite
 */
const { app } = require('./server');
const http = require('http');

const PORT = 5099;

const runTests = async () => {
  console.log('====================================================');
  console.log('🧪 Starting PathFinder AI Backend Automated Test Suite');
  console.log('====================================================');

  const server = http.createServer(app);
  await new Promise(resolve => server.listen(PORT, resolve));
  console.log(`[Test Runner] Test server listening on http://localhost:${PORT}`);

  const makeRequest = (path, method = 'GET', body = null) => {
    return new Promise((resolve, reject) => {
      const url = new URL(`http://localhost:${PORT}${path}`);
      const options = {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname + url.search,
        method: method,
        headers: {
          'Content-Type': 'application/json'
        }
      };

      const req = http.request(options, res => {
        let rawData = '';
        res.on('data', chunk => { rawData += chunk; });
        res.on('end', () => {
          try {
            const parsed = JSON.parse(rawData);
            resolve({ status: res.statusCode, body: parsed });
          } catch (e) {
            resolve({ status: res.statusCode, raw: rawData });
          }
        });
      });

      req.on('error', reject);

      if (body) {
        req.write(JSON.stringify(body));
      }
      req.end();
    });
  };

  let passed = 0;
  let failed = 0;

  const assert = (condition, name, details = '') => {
    if (condition) {
      console.log(`  ✅ PASS: ${name}`);
      passed++;
    } else {
      console.error(`  ❌ FAIL: ${name} - ${details}`);
      failed++;
    }
  };

  try {
    // 1. Health Check
    console.log('\n[1] Testing GET /api/health');
    const health = await makeRequest('/api/health');
    assert(health.status === 200 && health.body.success === true, 'Health check returns 200 OK');
    assert(health.body.data.status === 'ONLINE', 'Service status is ONLINE');

    // 2. GET Buildings
    console.log('\n[2] Testing GET /api/buildings');
    const buildings = await makeRequest('/api/buildings');
    assert(buildings.status === 200 && Array.isArray(buildings.body.data), 'Buildings list is an array');
    assert(buildings.body.data.length > 0, 'Returns at least 1 building (Engineering Block)');

    const testBuildingId = buildings.body.data[0].buildingId;

    // 3. GET Single Building
    console.log(`\n[3] Testing GET /api/buildings/${testBuildingId}`);
    const singleBld = await makeRequest(`/api/buildings/${testBuildingId}`);
    assert(singleBld.status === 200, 'Single building returns 200');
    assert(singleBld.body.data.buildingId === testBuildingId, `Building ID matches ${testBuildingId}`);

    // 4. GET Floors
    console.log(`\n[4] Testing GET /api/buildings/${testBuildingId}/floors`);
    const floors = await makeRequest(`/api/buildings/${testBuildingId}/floors`);
    assert(floors.status === 200 && floors.body.data.length >= 4, 'Returns 4 floors (Ground to Floor 3)');

    // 5. GET Locations
    console.log(`\n[5] Testing GET /api/buildings/${testBuildingId}/locations`);
    const locations = await makeRequest(`/api/buildings/${testBuildingId}/locations`);
    assert(locations.status === 200 && locations.body.data.length > 5, 'Returns locations for building');

    // 6. GET Navigation Nodes
    console.log(`\n[6] Testing GET /api/buildings/${testBuildingId}/nodes`);
    const nodes = await makeRequest(`/api/buildings/${testBuildingId}/nodes`);
    assert(nodes.status === 200 && nodes.body.data.length > 10, 'Returns navigation nodes');

    // 7. GET Navigation Edges
    console.log(`\n[7] Testing GET /api/buildings/${testBuildingId}/edges`);
    const edges = await makeRequest(`/api/buildings/${testBuildingId}/edges`);
    assert(edges.status === 200 && edges.body.data.length > 10, 'Returns navigation graph edges');

    // 8. Search Locations
    console.log('\n[8] Testing GET /api/locations/search?q=AI');
    const searchRes = await makeRequest('/api/locations/search?q=AI');
    assert(searchRes.status === 200, 'Search returns 200');
    assert(searchRes.body.data.some(l => l.name.includes('AI') || l.keywords.includes('ai lab')), 'Finds AI Laboratory');

    // 9. Route Calculation (Ground Entrance -> Floor 2 AI Lab)
    console.log('\n[9] Testing POST /api/navigation/route (Multi-Floor A* Calculation)');
    const routeRes = await makeRequest('/api/navigation/route', 'POST', {
      buildingId: 'univ',
      startNodeId: 'u_ent',
      targetNodeId: 'u_ai_lab',
      isWheelchairMode: false
    });
    assert(routeRes.status === 200, 'Route calculation returns 200 OK');
    assert(routeRes.body.data.totalDistanceMeters > 0, `Total distance calculated: ${routeRes.body.data.totalDistanceMeters}m`);
    assert(routeRes.body.data.instructions.length > 0, `Generated ${routeRes.body.data.instructions.length} turn-by-turn steps`);
    assert(routeRes.body.data.floorsTraversed.includes(0) && routeRes.body.data.floorsTraversed.includes(2), 'Traverses Floor 0 to Floor 2');

    // 10. Wheelchair Accessible Route Calculation
    console.log('\n[10] Testing POST /api/navigation/route with isWheelchairMode=true');
    const wheelchairRoute = await makeRequest('/api/navigation/route', 'POST', {
      buildingId: 'univ',
      startNodeId: 'u_ent',
      targetNodeId: 'u_ai_lab',
      isWheelchairMode: true
    });
    assert(wheelchairRoute.status === 200, 'Accessible route calculated successfully');
    assert(wheelchairRoute.body.data.pathNodes.some(n => n.category === 'ELEVATOR'), 'Accessible route uses Elevator instead of stairs');

    // 11. Error Handling: Missing Parameters
    console.log('\n[11] Testing POST /api/navigation/route Error Validation');
    const badRoute = await makeRequest('/api/navigation/route', 'POST', {
      buildingId: 'univ'
    });
    assert(badRoute.status === 400 && badRoute.body.success === false, 'Rejects missing startNodeId/targetNodeId with 400');

    // 12. 404 Route Not Found
    console.log('\n[12] Testing 404 Catch-All');
    const notFound = await makeRequest('/api/non-existent-route');
    assert(notFound.status === 404 && notFound.body.success === false, 'Returns 404 on invalid route');

  } catch (err) {
    console.error('[Test Error]:', err);
    failed++;
  } finally {
    server.close();
    console.log('\n====================================================');
    console.log(`📊 Test Results: ${passed} Passed, ${failed} Failed`);
    console.log('====================================================');
    process.exit(failed > 0 ? 1 : 0);
  }
};

runTests();
