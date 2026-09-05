# PathFinder AI — REST API Backend

A production-ready Node.js + Express + MongoDB backend engine powering **PathFinder AI** indoor navigation and wayfinding.

---

## 📁 Directory Structure

```
backend/
├── config/
│   └── db.js                 # MongoDB connection & reconnection handler
├── models/
│   ├── User.js               # User accounts, roles (USER, ADMIN), preferences
│   ├── Building.js           # Multi-floor facility metadata
│   ├── Floor.js              # Floor levels & blueprint coordinates
│   ├── Location.js           # POIs, departments, rooms, and search indexes
│   ├── NavNode.js            # Spatial graph nodes (X, Y, Floor, Category)
│   ├── NavEdge.js            # Graph edges with distance & accessibility
│   ├── NavigationSession.js  # Wayfinding sessions & analytics logs
│   ├── Favorite.js           # User bookmarked waypoints
│   └── SearchHistory.js      # Search analytics and query logs
├── controllers/
│   ├── healthController.js   # Uptime and service health telemetry
│   ├── buildingController.js # Building, floor, node & edge retrieval
│   ├── locationController.js # Location indexing and full-text search
│   └── navigationController.js# Wayfinding & shortest path generation
├── routes/
│   ├── index.js              # Master API router mounting under /api
│   ├── healthRoutes.js       # /api/health
│   ├── buildingRoutes.js     # /api/buildings/*
│   ├── locationRoutes.js     # /api/locations/*
│   └── navigationRoutes.js   # /api/navigation/*
├── middleware/
│   ├── errorHandler.js       # Centralized error handler with standardized JSON
│   ├── validateRequest.js    # Payload validation middleware
│   └── auth.js               # JWT bearer verification and RBAC
├── services/
│   ├── buildingService.js    # Building & graph data abstraction layer
│   ├── locationService.js    # POI search and keyword matching
│   └── navigationService.js  # Pathfinding coordinator & session tracker
├── algorithms/
│   └── astar.js              # A* 3D multi-floor graph pathfinding engine
├── utils/
│   ├── responseFormatter.js  # Consistent JSON envelope formatter
│   └── seedData.js           # University Engineering Block demo dataset
├── package.json
├── server.js                 # Express application entry point
├── test.js                   # Automated API integration test suite
└── .env.example              # Environment variables template
```

---

## 🛠️ Quickstart Installation & Run

### 1. Install Dependencies
```bash
cd backend
npm install
```

### 2. Configure Environment Variables
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

Edit `.env`:
```env
PORT=5000
NODE_ENV=development
MONGODB_URI=mongodb://localhost:27017/pathfinder_ai
JWT_SECRET=your_jwt_super_secret_key_change_in_production
CORS_ORIGIN=*
```

### 3. Seed Database (Optional)
```bash
npm run seed
```

### 4. Start Server
```bash
# Start in production mode
npm start

# Or start in development mode with nodemon
npm run dev
```

### 5. Run Automated Test Suite
```bash
npm test
```

---

## 📡 REST API Documentation

### Health Check
* **`GET /api/health`**
  ```json
  {
    "success": true,
    "message": "PathFinder AI system is healthy and operational.",
    "data": {
      "status": "ONLINE",
      "service": "PathFinder AI Backend Core Engine",
      "version": "1.0.0",
      "uptimeSeconds": 14.2
    }
  }
  ```

---

### Buildings & Floors
* **`GET /api/buildings`** — List all active facilities.
* **`GET /api/buildings/:id`** — Get single building details.
* **`GET /api/buildings/:id/floors`** — Get floors for building.
* **`GET /api/buildings/:id/locations`** — Get all indexed rooms and locations.
* **`GET /api/buildings/:id/nodes`** — Get all waypoints / graph nodes.
* **`GET /api/buildings/:id/edges`** — Get all walkable corridor connections.

---

### Location Search
* **`GET /api/locations/search?q=AI&buildingId=univ`**
  ```json
  {
    "success": true,
    "data": [
      {
        "locationId": "loc_u_ai_lab",
        "nodeId": "u_ai_lab",
        "buildingId": "univ",
        "floorNumber": 2,
        "name": "Artificial Intelligence & Robotics Lab",
        "code": "LAB-201",
        "category": "LABORATORY",
        "isAccessible": true,
        "coordinates": { "x": 220, "y": 200 }
      }
    ]
  }
  ```

---

### Navigation & A* Route Calculation
* **`POST /api/navigation/route`**
  * **Request Body:**
    ```json
    {
      "buildingId": "univ",
      "startNodeId": "u_ent",
      "targetNodeId": "u_ai_lab",
      "isWheelchairMode": true
    }
    ```
  * **Response:**
    ```json
    {
      "success": true,
      "message": "Optimal navigation route calculated successfully",
      "data": {
        "buildingId": "univ",
        "totalDistanceMeters": 115,
        "estimatedTimeMinutes": 2,
        "floorsTraversed": [0, 1, 2],
        "stepCount": 5,
        "pathNodeIds": ["u_ent", "u_rec", "u_g_el1", "u_1_el1", "u_2_el1", "u_ai_lab"],
        "instructions": [
          {
            "stepNumber": 1,
            "instruction": "Start at Main Campus Entrance (ENT-01)",
            "direction": "START",
            "floor": 0
          },
          {
            "stepNumber": 2,
            "instruction": "Continue straight through corridor past Central Reception & Help Desk",
            "direction": "STRAIGHT",
            "floor": 0
          },
          {
            "stepNumber": 3,
            "instruction": "Take Elevator from Floor 0 to Floor 1",
            "direction": "ELEVATOR_UP",
            "floor": 0
          },
          {
            "stepNumber": 4,
            "instruction": "Take Elevator from Floor 1 to Floor 2",
            "direction": "ELEVATOR_UP",
            "floor": 1
          },
          {
            "stepNumber": 5,
            "instruction": "Arrive at destination: Artificial Intelligence & Robotics Lab on Floor 2",
            "direction": "ARRIVE",
            "floor": 2
          }
        ]
      }
    }
    ```
