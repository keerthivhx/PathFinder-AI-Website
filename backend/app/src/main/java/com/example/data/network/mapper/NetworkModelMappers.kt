package com.example.data.network.mapper

import com.example.data.model.*
import com.example.data.network.dto.*

object NetworkModelMappers {

    fun mapCategory(categoryStr: String?): NodeCategory {
        if (categoryStr.isNullOrBlank()) return NodeCategory.GENERAL
        return try {
            NodeCategory.valueOf(categoryStr.trim().uppercase())
        } catch (e: Exception) {
            when (categoryStr.trim().uppercase()) {
                "LAB", "LABS" -> NodeCategory.LABORATORY
                "CLASS", "ROOM" -> NodeCategory.CLASSROOM
                "LIFT" -> NodeCategory.ELEVATOR
                "FIRE_EXIT", "EXIT" -> NodeCategory.EMERGENCY_EXIT
                "CANTEEN", "CAFE" -> NodeCategory.FOOD_COURT
                "OFFICE", "ADMIN" -> NodeCategory.FACULTY
                "TOILET", "WASHROOM", "WC" -> NodeCategory.RESTROOM
                else -> NodeCategory.GENERAL
            }
        }
    }

    fun mapEdgeType(edgeTypeStr: String?): EdgeType {
        if (edgeTypeStr.isNullOrBlank()) return EdgeType.CORRIDOR
        return try {
            EdgeType.valueOf(edgeTypeStr.trim().uppercase())
        } catch (e: Exception) {
            when (edgeTypeStr.trim().uppercase()) {
                "LIFT" -> EdgeType.ELEVATOR
                "STEPS" -> EdgeType.STAIRS
                "FIRE_EXIT" -> EdgeType.EMERGENCY_EXIT
                else -> EdgeType.CORRIDOR
            }
        }
    }

    fun FloorDto.toDomain(): FloorInfo {
        return FloorInfo(
            floorNumber = this.floorNumber,
            name = this.name,
            shortName = this.shortName ?: this.displayLabel ?: if (this.floorNumber == 0) "G" else "L${this.floorNumber}",
            buildingId = this.buildingId ?: "",
            mapWidth = this.mapWidth ?: 1000f,
            mapHeight = this.mapHeight ?: 1000f,
            mapScale = this.mapScale ?: 1.0f,
            mapAsset = this.mapAsset ?: "blueprint_vector",
            active = this.active ?: true
        )
    }

    fun FloorInfo.toDto(buildingId: String): FloorDto {
        return FloorDto(
            floorId = "${buildingId}_fl_${this.floorNumber}",
            buildingId = buildingId,
            floorNumber = this.floorNumber,
            name = this.name,
            displayLabel = this.shortName,
            shortName = this.shortName,
            mapWidth = this.mapWidth,
            mapHeight = this.mapHeight,
            mapScale = this.mapScale,
            mapAsset = this.mapAsset,
            active = this.active
        )
    }

    fun NavNodeDto.toDomain(): NavNode {
        return NavNode(
            id = this.nodeId,
            buildingId = this.buildingId,
            floor = this.floor,
            x = this.x,
            y = this.y,
            name = this.name,
            code = this.code,
            category = mapCategory(this.category),
            description = this.description ?: "",
            keywords = this.keywords ?: listOf(this.name.lowercase(), this.code.lowercase()),
            isAccessible = this.isAccessible ?: true,
            isEmergencyExit = this.isEmergencyExit ?: (mapCategory(this.category) == NodeCategory.EMERGENCY_EXIT),
            visualSignageHint = this.visualSignageHint ?: "${this.name} Sign",
            iconName = this.iconName ?: "place",
            active = this.active ?: true
        )
    }

    fun NavNode.toDto(): NavNodeDto {
        return NavNodeDto(
            nodeId = this.id,
            buildingId = this.buildingId,
            floor = this.floor,
            x = this.x,
            y = this.y,
            name = this.name,
            code = this.code,
            category = this.category.name,
            description = this.description,
            keywords = this.keywords,
            isAccessible = this.isAccessible,
            isEmergencyExit = this.isEmergencyExit,
            visualSignageHint = this.visualSignageHint,
            iconName = this.iconName,
            active = this.active
        )
    }

    fun LocationDto.toNavNode(): NavNode {
        return NavNode(
            id = this.nodeId,
            buildingId = this.buildingId,
            floor = this.floorNumber,
            x = this.coordinates?.x ?: 500f,
            y = this.coordinates?.y ?: 500f,
            name = this.name,
            code = this.code,
            category = mapCategory(this.category),
            description = this.description ?: "",
            keywords = this.keywords ?: listOf(this.name.lowercase(), this.code.lowercase()),
            isAccessible = this.isAccessible ?: true,
            isEmergencyExit = this.isEmergencyExit ?: false,
            visualSignageHint = "${this.name} Sign",
            iconName = "place",
            active = this.active ?: true
        )
    }

    fun NavNode.toLocationDto(): LocationDto {
        return LocationDto(
            locationId = "loc_${this.id}",
            nodeId = this.id,
            buildingId = this.buildingId,
            floorNumber = this.floor,
            name = this.name,
            code = this.code,
            category = this.category.name,
            description = this.description,
            keywords = this.keywords,
            isAccessible = this.isAccessible,
            isEmergencyExit = this.isEmergencyExit,
            coordinates = CoordinatesDto(this.x, this.y),
            active = this.active
        )
    }

    fun NavEdgeDto.toDomain(): NavEdge {
        return NavEdge(
            fromId = this.fromId,
            toId = this.toId,
            distanceMeters = this.distanceMeters,
            edgeType = mapEdgeType(this.edgeType),
            isWheelchairAccessible = this.isWheelchairAccessible ?: true,
            bidirectional = this.bidirectional ?: true,
            restricted = this.restricted ?: false,
            stairs = this.stairs ?: (mapEdgeType(this.edgeType) == EdgeType.STAIRS),
            elevator = this.elevator ?: (mapEdgeType(this.edgeType) == EdgeType.ELEVATOR),
            ramp = this.ramp ?: (mapEdgeType(this.edgeType) == EdgeType.RAMP)
        )
    }

    fun NavEdge.toDto(buildingId: String? = null): NavEdgeDto {
        return NavEdgeDto(
            edgeId = "edge_${fromId}_${toId}",
            buildingId = buildingId,
            fromId = this.fromId,
            toId = this.toId,
            distanceMeters = this.distanceMeters,
            edgeType = this.edgeType.name,
            isWheelchairAccessible = this.isWheelchairAccessible,
            bidirectional = this.bidirectional,
            restricted = this.restricted,
            stairs = this.stairs,
            elevator = this.elevator,
            ramp = this.ramp
        )
    }

    fun BuildingDto.toDomain(
        floors: List<FloorInfo>,
        nodes: List<NavNode>,
        edges: List<NavEdge>
    ): Building {
        return Building(
            id = this.buildingId,
            name = this.name,
            type = this.type ?: "University",
            description = this.description ?: "",
            icon = when (this.type?.lowercase()) {
                "hospital" -> "local_hospital"
                "airport" -> "flight"
                "shopping mall", "mall" -> "shopping_bag"
                else -> "apartment"
            },
            latitude = this.latitude ?: 12.9716,
            longitude = this.longitude ?: 77.5946,
            address = this.address ?: "Engineering Campus, Tech Park Drive",
            entrances = this.entrances?.map { it.toDomain() } ?: emptyList(),
            floors = floors.ifEmpty {
                listOf(FloorInfo(0, "Ground Floor", "G"))
            },
            nodes = nodes,
            edges = edges,
            code = this.code ?: this.buildingId,
            activeStatus = this.activeStatus ?: true,
            graphVersion = this.graphVersion ?: 1,
            createdAt = this.createdAt ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt ?: System.currentTimeMillis()
        )
    }

    fun Building.toDto(): BuildingDto {
        return BuildingDto(
            buildingId = this.id,
            name = this.name,
            code = this.code,
            type = this.type,
            description = this.description,
            address = this.address,
            latitude = this.latitude,
            longitude = this.longitude,
            entrances = this.entrances.map { it.toDto() },
            totalFloors = this.floors.size,
            activeStatus = this.activeStatus,
            graphVersion = this.graphVersion,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    fun BuildingEntranceDto.toDomain(): BuildingEntrance {
        return BuildingEntrance(
            id = this.entranceId,
            name = this.name,
            latitude = this.latitude,
            longitude = this.longitude,
            type = when (this.type?.uppercase()) {
                "ACCESSIBLE" -> EntranceType.ACCESSIBLE
                "GATE" -> EntranceType.GATE
                "EMERGENCY" -> EntranceType.EMERGENCY
                "NORTH" -> EntranceType.NORTH
                "SOUTH" -> EntranceType.SOUTH
                "EAST" -> EntranceType.EAST
                "WEST" -> EntranceType.WEST
                else -> EntranceType.MAIN
            },
            linkedNodeId = this.linkedNodeId,
            isWheelchairAccessible = this.isWheelchairAccessible,
            description = this.description ?: ""
        )
    }

    fun BuildingEntrance.toDto(): BuildingEntranceDto {
        return BuildingEntranceDto(
            entranceId = this.id,
            name = this.name,
            latitude = this.latitude,
            longitude = this.longitude,
            type = this.type.name,
            linkedNodeId = this.linkedNodeId,
            isWheelchairAccessible = this.isWheelchairAccessible,
            description = this.description
        )
    }

    fun BeaconDto.toDomain(): Beacon {
        return Beacon(
            beaconId = this.beaconId,
            buildingId = this.buildingId,
            floorId = this.floorId,
            nodeId = this.nodeId,
            x = this.x,
            y = this.y,
            major = this.major ?: 1,
            minor = this.minor ?: 1,
            txPower = this.txPower ?: -59,
            activeStatus = this.activeStatus ?: true,
            description = (this.metadata?.get("description") as? String) ?: "PathFinder BLE Anchor Beacon"
        )
    }

    fun Beacon.toDto(): BeaconDto {
        return BeaconDto(
            beaconId = this.beaconId,
            buildingId = this.buildingId,
            floorId = this.floor,
            nodeId = this.nodeId,
            x = this.x,
            y = this.y,
            major = this.major,
            minor = this.minor,
            txPower = this.txPower,
            activeStatus = this.activeStatus,
            metadata = mapOf("description" to this.description, "macAddress" to this.macAddress)
        )
    }

    fun QrPointDto.toDomain(): QrCalibrationPoint {
        return QrCalibrationPoint(
            calibrationId = this.calibrationId,
            buildingId = this.buildingId,
            floorId = this.floorId,
            nodeId = this.nodeId,
            x = this.x,
            y = this.y,
            active = this.active
        )
    }

    fun QrCalibrationPoint.toDto(): QrPointDto {
        return QrPointDto(
            calibrationId = this.calibrationId,
            buildingId = this.buildingId,
            floorId = this.floorId,
            nodeId = this.nodeId,
            x = this.x,
            y = this.y,
            active = this.active,
            qrPayload = this.qrPayload
        )
    }
}
