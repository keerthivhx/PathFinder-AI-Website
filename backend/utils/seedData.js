const Building = require('../models/Building');
const Floor = require('../models/Floor');
const Location = require('../models/Location');
const NavNode = require('../models/NavNode');
const NavEdge = require('../models/NavEdge');
const Beacon = require('../models/Beacon');

const demoBuildings = [
  {
    buildingId: 'univ',
    name: 'University Engineering Block',
    type: 'University',
    description: 'Faculty of Computer Science, AI Labs, Data Science Labs & Lecture Theatres',
    address: 'Campus North Wing, Tech Ave',
    totalFloors: 4,
    activeStatus: true,
    floors: [
      { floorNumber: 0, name: 'Ground Floor (Atrium & Services)', shortName: 'G' },
      { floorNumber: 1, name: 'Floor 1 (Computer Science Dept)', shortName: 'L1' },
      { floorNumber: 2, name: 'Floor 2 (AI & Advanced Research Labs)', shortName: 'L2' },
      { floorNumber: 3, name: 'Floor 3 (Data Science & Auditorium)', shortName: 'L3' }
    ],
    nodes: [
      // Floor 0 - Ground
      {
        nodeId: 'u_ent',
        buildingId: 'univ',
        floor: 0,
        x: 500,
        y: 860,
        name: 'Main Campus Entrance',
        code: 'ENT-01',
        category: 'ENTRANCE',
        description: 'Main campus security check & entrance portal',
        keywords: ['entrance', 'gate', 'entry', 'door', 'security', 'main gate'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Glass double doors with PathFinder digital kiosk',
        iconName: 'door_front'
      },
      {
        nodeId: 'u_rec',
        buildingId: 'univ',
        floor: 0,
        x: 500,
        y: 650,
        name: 'Central Reception & Help Desk',
        code: 'REC-01',
        category: 'RECEPTION',
        description: 'Visitor badges, inquiry desk, campus maps',
        keywords: ['reception', 'help desk', 'inquiry', 'front desk', 'info'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Curved wooden desk with LED reception sign',
        iconName: 'desk'
      },
      {
        nodeId: 'u_adm',
        buildingId: 'univ',
        floor: 0,
        x: 240,
        y: 650,
        name: 'Principal & Administration Office',
        code: 'ADM-101',
        category: 'FACULTY',
        description: 'Dean office, registrar & academic administration',
        keywords: ['principal', 'director', 'admin', 'registrar', 'dean', 'administration'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Gold plaque: Administration Directorate',
        iconName: 'badge'
      },
      {
        nodeId: 'u_caf',
        buildingId: 'univ',
        floor: 0,
        x: 760,
        y: 650,
        name: 'Campus Cafeteria & Food Court',
        code: 'CAF-01',
        category: 'FOOD_COURT',
        description: 'Hot meals, snacks, coffee bar & dining hall',
        keywords: ['cafeteria', 'canteen', 'food', 'coffee', 'lunch', 'snacks', 'eat'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Cafeteria marquee over glass dining area',
        iconName: 'restaurant'
      },
      {
        nodeId: 'u_g_el1',
        buildingId: 'univ',
        floor: 0,
        x: 500,
        y: 420,
        name: 'Central Elevator Shaft (Ground)',
        code: 'ELV-01',
        category: 'ELEVATOR',
        description: 'High-speed elevator connecting Floors G, 1, 2, 3',
        keywords: ['elevator', 'lift', 'wheelchair', 'level change'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Brushed steel elevator doors with directional arrows',
        iconName: 'elevator'
      },
      {
        nodeId: 'u_g_st1',
        buildingId: 'univ',
        floor: 0,
        x: 320,
        y: 420,
        name: 'North Staircase (Ground)',
        code: 'STR-01',
        category: 'STAIRS',
        description: 'Wide stairway leading to Floors 1, 2, 3',
        keywords: ['stairs', 'steps', 'staircase', 'walkway'],
        isAccessible: false,
        isEmergencyExit: true,
        visualSignageHint: 'Blue handrails stairway entrance',
        iconName: 'stairs'
      },
      {
        nodeId: 'u_g_ex1',
        buildingId: 'univ',
        floor: 0,
        x: 880,
        y: 420,
        name: 'East Emergency Fire Exit',
        code: 'EMG-01',
        category: 'EMERGENCY_EXIT',
        description: 'Direct outdoor fire escape route and assembly point access',
        keywords: ['emergency', 'exit', 'fire escape', 'evacuation', 'danger'],
        isAccessible: true,
        isEmergencyExit: true,
        visualSignageHint: 'Green illuminated EXIT sign with panic push bar',
        iconName: 'emergency'
      },

      // Floor 1 - Computer Science Dept
      {
        nodeId: 'u_1_el1',
        buildingId: 'univ',
        floor: 1,
        x: 500,
        y: 420,
        name: 'Central Elevator Shaft (Floor 1)',
        code: 'ELV-02',
        category: 'ELEVATOR',
        description: 'Floor 1 elevator lobby',
        keywords: ['elevator', 'lift', 'floor 1', 'level 1'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Elevator door Floor 1',
        iconName: 'elevator'
      },
      {
        nodeId: 'u_1_st1',
        buildingId: 'univ',
        floor: 1,
        x: 320,
        y: 420,
        name: 'North Staircase (Floor 1)',
        code: 'STR-02',
        category: 'STAIRS',
        description: 'Floor 1 stairway portal',
        keywords: ['stairs', 'steps', 'level 1'],
        isAccessible: false,
        isEmergencyExit: true,
        visualSignageHint: 'Floor 1 Stairway Door',
        iconName: 'stairs'
      },
      {
        nodeId: 'u_cs_dept',
        buildingId: 'univ',
        floor: 1,
        x: 220,
        y: 220,
        name: 'Computer Science Department',
        code: 'CS-101',
        category: 'FACULTY',
        description: 'Department Head Office, HOD CS & Faculty Cabins',
        keywords: ['cs department', 'computer science', 'hod', 'faculty', 'professors'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'CS Department directory board',
        iconName: 'computer'
      },
      {
        nodeId: 'u_cs_lab1',
        buildingId: 'univ',
        floor: 1,
        x: 780,
        y: 220,
        name: 'Software Engineering Laboratory',
        code: 'LAB-102',
        category: 'LABORATORY',
        description: '60 high-spec Linux terminals & development workstations',
        keywords: ['software lab', 'coding lab', 'linux', 'computers', 'lab 102'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Lab 102 illuminated sign',
        iconName: 'terminal'
      },
      {
        nodeId: 'u_1_rest',
        buildingId: 'univ',
        floor: 1,
        x: 880,
        y: 420,
        name: 'Floor 1 Restrooms (All-Gender / Accessible)',
        code: 'WC-101',
        category: 'RESTROOM',
        description: 'ADA compliant restrooms with touchless amenities',
        keywords: ['restroom', 'toilet', 'washroom', 'wc', 'bathroom'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Braille tactile Restroom sign',
        iconName: 'wc'
      },

      // Floor 2 - AI Labs & Research
      {
        nodeId: 'u_2_el1',
        buildingId: 'univ',
        floor: 2,
        x: 500,
        y: 420,
        name: 'Central Elevator Shaft (Floor 2)',
        code: 'ELV-03',
        category: 'ELEVATOR',
        description: 'Floor 2 elevator lobby',
        keywords: ['elevator', 'lift', 'floor 2', 'level 2'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Elevator door Floor 2',
        iconName: 'elevator'
      },
      {
        nodeId: 'u_2_st1',
        buildingId: 'univ',
        floor: 2,
        x: 320,
        y: 420,
        name: 'North Staircase (Floor 2)',
        code: 'STR-03',
        category: 'STAIRS',
        description: 'Floor 2 stairway portal',
        keywords: ['stairs', 'steps', 'level 2'],
        isAccessible: false,
        isEmergencyExit: true,
        visualSignageHint: 'Floor 2 Stairway Door',
        iconName: 'stairs'
      },
      {
        nodeId: 'u_ai_lab',
        buildingId: 'univ',
        floor: 2,
        x: 220,
        y: 200,
        name: 'Artificial Intelligence & Robotics Lab',
        code: 'LAB-201',
        category: 'LABORATORY',
        description: 'GPU clusters, neural network training rigs & robotics testing arena',
        keywords: ['ai lab', 'artificial intelligence', 'robotics', 'machine learning', 'gpu', 'neural network', 'lab 201'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Blue neon: AI Research Centre',
        iconName: 'psychology'
      },
      {
        nodeId: 'u_place',
        buildingId: 'univ',
        floor: 2,
        x: 780,
        y: 200,
        name: 'Career Placement & Internship Cell',
        code: 'PLC-202',
        category: 'FACULTY',
        description: 'Corporate interview rooms, counseling & recruitment coordinator',
        keywords: ['placement', 'jobs', 'internships', 'career', 'interviews', 'placement cell'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Placement Cell corporate portal',
        iconName: 'work'
      },

      // Floor 3 - Data Science & Auditorium
      {
        nodeId: 'u_3_el1',
        buildingId: 'univ',
        floor: 3,
        x: 500,
        y: 420,
        name: 'Central Elevator Shaft (Floor 3)',
        code: 'ELV-04',
        category: 'ELEVATOR',
        description: 'Floor 3 elevator lobby',
        keywords: ['elevator', 'lift', 'floor 3', 'level 3'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Elevator door Floor 3',
        iconName: 'elevator'
      },
      {
        nodeId: 'u_3_st1',
        buildingId: 'univ',
        floor: 3,
        x: 320,
        y: 420,
        name: 'North Staircase (Floor 3)',
        code: 'STR-04',
        category: 'STAIRS',
        description: 'Floor 3 stairway portal',
        keywords: ['stairs', 'steps', 'level 3'],
        isAccessible: false,
        isEmergencyExit: true,
        visualSignageHint: 'Floor 3 Stairway Door',
        iconName: 'stairs'
      },
      {
        nodeId: 'u_ds_lab',
        buildingId: 'univ',
        floor: 3,
        x: 220,
        y: 200,
        name: 'Data Science & Cloud Computing Lab',
        code: 'LAB-301',
        category: 'LABORATORY',
        description: 'Big Data analysis clusters and cloud computing laboratory',
        keywords: ['data science', 'big data', 'cloud computing', 'analytics', 'lab 301'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Data Science Lab plaque',
        iconName: 'cloud'
      },
      {
        nodeId: 'u_aud',
        buildingId: 'univ',
        floor: 3,
        x: 780,
        y: 200,
        name: 'Main Grand Auditorium & Seminar Hall',
        code: 'AUD-302',
        category: 'AUDITORIUM',
        description: '500-seat auditorium with Dolby audio, stage & lecture theater',
        keywords: ['auditorium', 'seminar hall', 'conference', 'theater', 'hall 302', 'events'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Auditorium marquee over acoustic double doors',
        iconName: 'theater_comedy'
      },
      {
        nodeId: 'u_lib',
        buildingId: 'univ',
        floor: 3,
        x: 500,
        y: 180,
        name: 'Central Digital Research Library',
        code: 'LIB-303',
        category: 'LIBRARY',
        description: 'Quiet study pods, IEEE database terminals, book stacks & archives',
        keywords: ['library', 'books', 'quiet study', 'journals', 'reading room'],
        isAccessible: true,
        isEmergencyExit: false,
        visualSignageHint: 'Library glass entrance with RFID security arch',
        iconName: 'menu_book'
      }
    ],
    edges: [
      // Floor 0 Intraground Connections
      { fromId: 'u_ent', toId: 'u_rec', distanceMeters: 25, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_rec', toId: 'u_adm', distanceMeters: 28, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_rec', toId: 'u_caf', distanceMeters: 30, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_rec', toId: 'u_g_el1', distanceMeters: 22, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_rec', toId: 'u_g_st1', distanceMeters: 26, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_rec', toId: 'u_g_ex1', distanceMeters: 38, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_g_el1', toId: 'u_g_st1', distanceMeters: 18, edgeType: 'CORRIDOR', isWheelchairAccessible: true },

      // Floor 1 Connections
      { fromId: 'u_1_el1', toId: 'u_1_st1', distanceMeters: 18, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_1_el1', toId: 'u_cs_dept', distanceMeters: 35, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_1_el1', toId: 'u_cs_lab1', distanceMeters: 35, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_1_el1', toId: 'u_1_rest', distanceMeters: 38, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_1_st1', toId: 'u_cs_dept', distanceMeters: 25, edgeType: 'CORRIDOR', isWheelchairAccessible: true },

      // Floor 2 Connections
      { fromId: 'u_2_el1', toId: 'u_2_st1', distanceMeters: 18, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_2_el1', toId: 'u_ai_lab', distanceMeters: 36, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_2_el1', toId: 'u_place', distanceMeters: 36, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_2_st1', toId: 'u_ai_lab', distanceMeters: 26, edgeType: 'CORRIDOR', isWheelchairAccessible: true },

      // Floor 3 Connections
      { fromId: 'u_3_el1', toId: 'u_3_st1', distanceMeters: 18, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_3_el1', toId: 'u_ds_lab', distanceMeters: 36, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_3_el1', toId: 'u_aud', distanceMeters: 36, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_3_el1', toId: 'u_lib', distanceMeters: 24, edgeType: 'CORRIDOR', isWheelchairAccessible: true },
      { fromId: 'u_3_st1', toId: 'u_ds_lab', distanceMeters: 26, edgeType: 'CORRIDOR', isWheelchairAccessible: true },

      // Multi-Floor Elevator Vertical Shaft Edges (Accessible)
      { fromId: 'u_g_el1', toId: 'u_1_el1', distanceMeters: 12, edgeType: 'ELEVATOR', isWheelchairAccessible: true },
      { fromId: 'u_1_el1', toId: 'u_2_el1', distanceMeters: 12, edgeType: 'ELEVATOR', isWheelchairAccessible: true },
      { fromId: 'u_2_el1', toId: 'u_3_el1', distanceMeters: 12, edgeType: 'ELEVATOR', isWheelchairAccessible: true },

      // Multi-Floor Staircase Vertical Edges (Non-Accessible)
      { fromId: 'u_g_st1', toId: 'u_1_st1', distanceMeters: 15, edgeType: 'STAIRS', isWheelchairAccessible: false },
      { fromId: 'u_1_st1', toId: 'u_2_st1', distanceMeters: 15, edgeType: 'STAIRS', isWheelchairAccessible: false },
      { fromId: 'u_2_st1', toId: 'u_3_st1', distanceMeters: 15, edgeType: 'STAIRS', isWheelchairAccessible: false }
    ]
  }
];

/**
 * Seed Database with Demo Buildings & Locations
 */
const seedDatabase = async () => {
  try {
    console.log('[Seed] Starting database seed process...');

    for (const bldData of demoBuildings) {
      // 1. Upsert Building
      await Building.findOneAndUpdate(
        { buildingId: bldData.buildingId },
        {
          buildingId: bldData.buildingId,
          name: bldData.name,
          type: bldData.type,
          description: bldData.description,
          address: bldData.address,
          totalFloors: bldData.totalFloors,
          activeStatus: bldData.activeStatus
        },
        { upsert: true, new: true }
      );

      // 2. Upsert Floors
      for (const floor of bldData.floors) {
        await Floor.findOneAndUpdate(
          { buildingId: bldData.buildingId, floorNumber: floor.floorNumber },
          {
            buildingId: bldData.buildingId,
            floorNumber: floor.floorNumber,
            name: floor.name,
            shortName: floor.shortName
          },
          { upsert: true, new: true }
        );
      }

      // 3. Upsert NavNodes & Locations
      for (const node of bldData.nodes) {
        await NavNode.findOneAndUpdate(
          { nodeId: node.nodeId },
          { ...node },
          { upsert: true, new: true }
        );

        await Location.findOneAndUpdate(
          { locationId: `loc_${node.nodeId}` },
          {
            locationId: `loc_${node.nodeId}`,
            nodeId: node.nodeId,
            buildingId: node.buildingId,
            floorNumber: node.floor,
            name: node.name,
            code: node.code,
            category: node.category,
            description: node.description,
            keywords: node.keywords,
            isAccessible: node.isAccessible,
            isEmergencyExit: node.isEmergencyExit,
            coordinates: { x: node.x, y: node.y }
          },
          { upsert: true, new: true }
        );
      }

      // 4. Upsert Edges
      for (const edge of bldData.edges) {
        await NavEdge.findOneAndUpdate(
          { buildingId: bldData.buildingId, fromId: edge.fromId, toId: edge.toId },
          {
            buildingId: bldData.buildingId,
            fromId: edge.fromId,
            toId: edge.toId,
            distanceMeters: edge.distanceMeters,
            edgeType: edge.edgeType,
            isWheelchairAccessible: edge.isWheelchairAccessible,
            bidirectional: true
          },
          { upsert: true, new: true }
        );
      }
    }

    // 5. Seed Demo Beacons for Engineering Block (Phase 7 BLE positioning)
    const demoBeacons = [
      { beaconId: 'BEACON-GF-01', buildingId: 'univ', floorId: 0, nodeId: 'u_ent', x: 500, y: 860, major: 101, minor: 1, txPower: -59, activeStatus: true, metadata: { description: 'Main Gate Entrance Beacon' } },
      { beaconId: 'BEACON-GF-02', buildingId: 'univ', floorId: 0, nodeId: 'u_rec', x: 500, y: 650, major: 101, minor: 2, txPower: -59, activeStatus: true, metadata: { description: 'Central Reception Foyer Beacon' } },
      { beaconId: 'BEACON-GF-03', buildingId: 'univ', floorId: 0, nodeId: 'u_elv0', x: 500, y: 350, major: 101, minor: 3, txPower: -59, activeStatus: true, metadata: { description: 'Ground Floor Elevator Lobby Beacon' } },
      { beaconId: 'BEACON-F1-01', buildingId: 'univ', floorId: 1, nodeId: 'u_elv1', x: 500, y: 350, major: 102, minor: 1, txPower: -59, activeStatus: true, metadata: { description: 'Floor 1 CS Department Elevator Beacon' } },
      { beaconId: 'BEACON-F2-01', buildingId: 'univ', floorId: 2, nodeId: 'u_cslab', x: 240, y: 250, major: 103, minor: 1, txPower: -59, activeStatus: true, metadata: { description: 'Floor 2 AI Laboratory Beacon' } },
      { beaconId: 'BEACON-F2-02', buildingId: 'univ', floorId: 2, nodeId: 'u_elv2', x: 500, y: 350, major: 103, minor: 2, txPower: -59, activeStatus: true, metadata: { description: 'Floor 2 Research Wing Elevator Beacon' } }
    ];

    for (const b of demoBeacons) {
      await Beacon.findOneAndUpdate(
        { beaconId: b.beaconId },
        { ...b },
        { upsert: true, new: true }
      );
    }

    console.log('[Seed] Database seeding completed successfully with BLE beacons.');
  } catch (error) {
    console.error('[Seed Error] Failed to seed database:', error.message);
  }
};

module.exports = {
  demoBuildings,
  seedDatabase
};
