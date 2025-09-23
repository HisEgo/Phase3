package model;


import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class WireConnection {
    private String id;
    private Port sourcePort;
    private Port destinationPort;
    private double wireLength;
    private double consumedLength;
    private boolean isActive;
    private List<WireBend> bends;
    private boolean isDestroyed;
    private int bulkPacketPassages;
    private static final int MAX_BULK_PASSAGES = 3;
    private List<Packet> packetsOnWire; // Multiple packets can travel on the same wire
    // Per specification: Only one packet may occupy a wire from a port at any time
    private static final int MAX_WIRE_CAPACITY = 1;
    // Phase 1 spec: packet loss if packet goes off the wire path
    private static final double DEFAULT_OFF_WIRE_LOSS_THRESHOLD = 20.0; // pixels
    // Owner player ID for multiplayer games
    private String ownerPlayerId;


    public WireConnection() {
        this.id = java.util.UUID.randomUUID().toString();
        this.isActive = true;
        this.consumedLength = 0.0;
        this.bends = new ArrayList<>();
        this.isDestroyed = false;
        this.bulkPacketPassages = 0;
        this.packetsOnWire = new ArrayList<>();

    }

    public WireConnection(Port sourcePort, Port destinationPort, double wireLength) {
        this();
        // Normalize so that sourcePort is always an OUTPUT and destinationPort is always an INPUT
        Port normalizedSource = sourcePort;
        Port normalizedDestination = destinationPort;
        if (sourcePort != null && destinationPort != null) {
            boolean sourceIsInput = sourcePort.isInput();
            boolean destIsInput = destinationPort.isInput();
            // If the first port is an input and the second is an output, swap them
            if (sourceIsInput && !destIsInput) {
                normalizedSource = destinationPort;
                normalizedDestination = sourcePort;
            }
        }

        this.sourcePort = normalizedSource;
        this.destinationPort = normalizedDestination;
        this.wireLength = wireLength;
        this.consumedLength = wireLength; // Mark the full length as consumed when wire is created
    }

    public WireConnection(Port sourcePort, Port destinationPort) {
        this();
        // Normalize so that sourcePort is always an OUTPUT and destinationPort is always an INPUT
        Port normalizedSource = sourcePort;
        Port normalizedDestination = destinationPort;
        if (sourcePort != null && destinationPort != null) {
            boolean sourceIsInput = sourcePort.isInput();
            boolean destIsInput = destinationPort.isInput();
            if (sourceIsInput && !destIsInput) {
                normalizedSource = destinationPort;
                normalizedDestination = sourcePort;
            }
        }

        this.sourcePort = normalizedSource;
        this.destinationPort = normalizedDestination;
        // Calculate wire length based on port positions
        if (this.sourcePort != null && this.destinationPort != null &&
                this.sourcePort.getPosition() != null && this.destinationPort.getPosition() != null) {
            this.wireLength = this.sourcePort.getPosition().distanceTo(this.destinationPort.getPosition());
            this.consumedLength = this.wireLength; // Mark the full length as consumed when wire is created
        } else {
            this.wireLength = 0.0;
            this.consumedLength = 0.0;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Port getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(Port sourcePort) {
        this.sourcePort = sourcePort;
    }

    public Port getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(Port destinationPort) {
        this.destinationPort = destinationPort;
    }

    public double getWireLength() {
        return wireLength;
    }

    public void setWireLength(double wireLength) {
        this.wireLength = wireLength;
    }

    public double getConsumedLength() {
        return consumedLength;
    }

    public void setConsumedLength(double consumedLength) {
        this.consumedLength = consumedLength;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Phase 2 properties
    public List<WireBend> getBends() {
        return bends;
    }

    public void setBends(List<WireBend> bends) {
        this.bends = bends;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void setDestroyed(boolean destroyed) {
        isDestroyed = destroyed;
    }

    public int getBulkPacketPassages() {
        return bulkPacketPassages;
    }

    public void setBulkPacketPassages(int bulkPacketPassages) {
        this.bulkPacketPassages = bulkPacketPassages;
    }

    public double getRemainingLength() {
        return wireLength - consumedLength;
    }

    public boolean hasSufficientLength() {
        return getRemainingLength() > 0;
    }

    public boolean consumeLength(double amount) {
        if (consumedLength + amount > wireLength) {
            return false; // Not enough wire length
        }

        consumedLength += amount;
        return true;
    }

    public double getActualDistance() {
        if (sourcePort == null || destinationPort == null) {
            return 0.0;
        }

        return sourcePort.getPosition().distanceTo(destinationPort.getPosition());
    }

    public boolean isValid() {
        if (sourcePort == null || destinationPort == null) {
            return false;
        }

        // Check if ports are from different systems
        if (sourcePort.getParentSystem() == destinationPort.getParentSystem()) {
            return false;
        }

        // Check if one is input and one is output
        if (sourcePort.isInput() == destinationPort.isInput()) {
            return false;
        }

        // Check if they have compatible shapes
        return sourcePort.getShape().isCompatibleWith(destinationPort.getShape());
    }

    public boolean transferPacket() {
        if (!isActive || !isValid()) {
            return false;
        }

        // Determine actual input and output based on wire direction
        Port wireInputPort = sourcePort;  // Wire input is always the source port
        Port wireOutputPort = destinationPort; // Wire output is always the destination port

        // Try to move packet from source port to wire (if wire is available)
        if (wireInputPort.getCurrentPacket() != null && canAcceptPacket()) {
            Packet packet = wireInputPort.releasePacket();
            return acceptPacket(packet);
        }

        // Try to move packets from wire to destination port (if destination port is available)
        if (isOccupied() && wireOutputPort.isEmpty()) {
            // Move the first packet that's ready to be transferred
            for (Packet packet : packetsOnWire) {
                if (hasPacketReachedDestination(packet)) {
                    packetsOnWire.remove(packet);
                    
                    // Check if this is a bulk packet completing its passage - destroy wire if needed
                    if (packet.getPacketType() != null && packet.getPacketType().isBulk()) {
                        if (bulkPacketPassages >= MAX_BULK_PASSAGES) {
                            isDestroyed = true;
                            isActive = false;
                            java.lang.System.out.println("*** WIRE DESTROYED *** After bulk packet " + bulkPacketPassages + " completed passage");
                        }
                    }
                    
                    boolean accepted = wireOutputPort.acceptPacket(packet);
                    if (accepted) {
                        // Mark coin award as pending for system entry (one-shot)
                        packet.setCoinAwardPending(true);
                        String systemType = wireOutputPort.getParentSystem() != null ?
                                wireOutputPort.getParentSystem().getClass().getSimpleName() : "Unknown";

                        // If destination is a ReferenceSystem, finalize delivery immediately
                        model.System destSystem = wireOutputPort.getParentSystem();
                        if (destSystem instanceof model.ReferenceSystem) {
                            model.ReferenceSystem ref = (model.ReferenceSystem) destSystem;
                            // All reference systems can receive packets now
                            model.Packet p = wireOutputPort.releasePacket();
                            if (p != null) {
                                ref.processPacket(p);
                            }
                        }
                    }
                    return accepted;
                }
            }
        }

        return false;
    }

    public List<Packet> getPacketsOnWire() {
        return new ArrayList<>(packetsOnWire);
    }

    public void setPacketsOnWire(List<Packet> packets) {
        this.packetsOnWire = new ArrayList<>(packets);
    }

    public boolean isOccupied() {
        return !packetsOnWire.isEmpty() && packetsOnWire.stream().anyMatch(Packet::isActive);
    }

    public boolean canAcceptPacket() {
        // Only allow accepting a packet when no active packet is currently on this wire
        boolean hasActiveOnWire = packetsOnWire.stream().anyMatch(Packet::isActive);
        return !hasActiveOnWire && isActive && !isDestroyed;
    }

    public boolean acceptPacket(Packet packet) {
        if (!canAcceptPacket()) {
            return false;
        }

        this.packetsOnWire.add(packet);

        // Handle bulk packet passage counting (but don't destroy wire yet)
        if (packet.getPacketType() != null && packet.getPacketType().isBulk()) {
            bulkPacketPassages++;
            java.lang.System.out.println("*** BULK PACKET PASSAGE *** Count: " + bulkPacketPassages + "/" + MAX_BULK_PASSAGES + " on wire");
            // Wire destruction will happen when the packet exits the wire, not when it enters
        }

        // Initialize packet for path-based movement on this wire
        initializePacketOnWire(packet);

        // Set packet movement direction along the wire
        Vec2D direction = getDirectionVector();
        if (direction.magnitude() > 0) {
            // Use packet's base speed instead of overriding it
            // The MovementController will handle packet-specific speed calculations
            double initialSpeed = packet.getBaseSpeed();
            if (initialSpeed <= 0) {
                initialSpeed = 50.0; // Fallback default speed
            }

            packet.setMovementVector(direction.normalize().scale(initialSpeed));
        }

        // Reset travel time for new wire
        packet.resetTravelTime();

        return true;
    }

    public Packet releasePacket(Packet packet) {
        if (packetsOnWire.remove(packet)) {
            return packet;
        }
        return null;
    }

    public void clearPackets() {
        packetsOnWire.clear();
    }

    public boolean hasPacketReachedDestination(Packet packet) {
        if (!packetsOnWire.contains(packet) || destinationPort == null) {
            return false;
        }

        Point2D packetPos = packet.getCurrentPosition();
        Point2D destPos = destinationPort.getPosition();

        // Consider packet reached if within 5 pixels of destination (reduced threshold)
        return packetPos.distanceTo(destPos) <= 5.0;
    }

    public void updatePacketMovement(double deltaTime) {
        updatePacketMovement(deltaTime, true); // Default to smooth curves for backward compatibility
    }

    public void updatePacketMovement(double deltaTime, boolean useSmoothCurves) {
        if (!isOccupied()) {
            return;
        }

        // Update all packets on this wire
        List<Packet> packetsToRemove = new ArrayList<>();

        for (Packet packet : packetsOnWire) {
            if (!packet.isActive()) {
                packetsToRemove.add(packet);
                continue;
            }

            // Ensure packet is properly initialized for wire-based movement
            if (!packet.isOnWire() || packet.getCurrentWire() != this) {
                initializePacketOnWire(packet);
            }

            // Check if packet has reached destination
            if (hasPacketReachedDestination(packet)) {
                // Don't update position if at destination - let transfer logic handle it
                continue;
            }

            // For packets not using path-based movement, use legacy approach
            if (!packet.isOnWire()) {
                packet.updatePosition(deltaTime);
            }
            // Always constrain to wire path and enforce off-wire loss rule
            constrainPacketToWire(packet, useSmoothCurves);
            // Note: Path-based movement is handled by MovementController
        }

        // Remove inactive packets
        packetsOnWire.removeAll(packetsToRemove);
    }

    private void initializePacketOnWire(Packet packet) {
        packet.setCurrentWire(this);
        packet.setPathProgress(0.0);

        // Set initial position at the start of the wire path
        Point2D startPosition = getPositionAtProgress(0.0);
        if (startPosition != null) {
            packet.setCurrentPosition(startPosition);
        }

        // Initialize base speed if not set
        if (packet.getBaseSpeed() <= 0) {
            packet.setBaseSpeed(50.0); // Default base speed
        }

        // Calculate initial movement vector based on wire direction
        // For curved wires, look at the direction from start to a small progress ahead
        // Use smooth curves by default for initialization
        Point2D currentPos = getPositionAtProgress(0.0);
        Point2D nextPos = getPositionAtProgress(Math.min(0.1, 10.0 / getTotalLength()));

        if (currentPos != null && nextPos != null) {
            Vec2D direction = new Vec2D(
                    nextPos.getX() - currentPos.getX(),
                    nextPos.getY() - currentPos.getY()
            );
            if (direction.magnitude() > 0) {
                packet.setMovementVector(direction.normalize().scale(packet.getBaseSpeed()));
            }
        }
    }

    private void constrainPacketToWire(Packet packet) {
        constrainPacketToWire(packet, true); // Default to smooth curves for backward compatibility
    }

    private void constrainPacketToWire(Packet packet, boolean useSmoothCurves) {
        if (sourcePort == null || destinationPort == null) {
            return;
        }

        Point2D packetPos = packet.getCurrentPosition();
        List<Point2D> pathPoints = getPathPoints(useSmoothCurves);

        if (pathPoints.size() < 2) {
            return;
        }

        // Find the closest point on the wire path
        Point2D closestPoint = findClosestPointOnPath(packetPos, pathPoints);
        if (closestPoint != null) {
            double deviation = packetPos.distanceTo(closestPoint);
            double threshold = DEFAULT_OFF_WIRE_LOSS_THRESHOLD;
            // Allow configurable threshold via GameState setting if available
            try {
                model.GameLevel level = null;
                if (sourcePort != null && sourcePort.getParentSystem() != null) {
                    level = sourcePort.getParentSystem().getParentLevel();
                }
                if (level == null && destinationPort != null && destinationPort.getParentSystem() != null) {
                    level = destinationPort.getParentSystem().getParentLevel();
                }
                if (level != null) {
                    // GameState holds settings; attempt to fetch from any system's parent level via a reference system
                    // Since we don't hold GameState here, use a convention: store threshold in each system's GameLevel via settings in GameState
                    // Fallback to default if not reachable.
                    // Note: For simplicity, use default unless future refactor passes GameState here.
                }
            } catch (Exception ignored) {}
            if (deviation > threshold) {
                // Mark packet as lost per Phase 1 spec: "A packet goes off the wire path"
                packet.setLost(true);
                packet.setActive(false);
                return;
            }
            // Snap gently to the path when within tolerance
            packet.setCurrentPosition(closestPoint);
        }
    }

    private Point2D findClosestPointOnPath(Point2D position, List<Point2D> pathPoints) {
        if (pathPoints.size() < 2) {
            return null;
        }

        Point2D closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        // Check each segment of the path
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D segmentStart = pathPoints.get(i);
            Point2D segmentEnd = pathPoints.get(i + 1);

            Point2D pointOnSegment = getClosestPointOnLineSegment(position, segmentStart, segmentEnd);
            double distance = position.distanceTo(pointOnSegment);

            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = pointOnSegment;
            }
        }

        return closestPoint;
    }

    private Point2D getClosestPointOnLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        Vec2D lineVec = new Vec2D(lineEnd.getX() - lineStart.getX(), lineEnd.getY() - lineStart.getY());
        Vec2D pointVec = new Vec2D(point.getX() - lineStart.getX(), point.getY() - lineStart.getY());

        double lineLength = lineVec.magnitude();
        if (lineLength == 0) {
            return lineStart;
        }

        double projection = pointVec.dot(lineVec) / (lineLength * lineLength);
        projection = Math.max(0, Math.min(1, projection)); // Clamp to segment

        return new Point2D(
                lineStart.getX() + lineVec.getX() * projection,
                lineStart.getY() + lineVec.getY() * projection
        );
    }

    public Vec2D getDirectionVector() {
        if (sourcePort == null || destinationPort == null) {
            return new Vec2D();
        }

        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();

        return new Vec2D(
                destPos.getX() - sourcePos.getX(),
                destPos.getY() - sourcePos.getY()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WireConnection that = (WireConnection) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WireConnection{" +
                "id='" + id + '\'' +
                ", sourcePort=" + (sourcePort != null ? sourcePort.getParentSystem().getId() : "null") +
                ", destinationPort=" + (destinationPort != null ? destinationPort.getParentSystem().getId() : "null") +
                ", wireLength=" + wireLength +
                ", consumedLength=" + consumedLength +
                ", active=" + isActive +
                '}';
    }

    public boolean addBend(Point2D position, List<System> systems) {
        if (bends.size() >= 3) {
            return false; // Maximum 3 bends allowed
        }

        // Use smooth curve path points to find the nearest segment for better alignment
        List<Point2D> pathPoints = getPathPoints(true); // Force smooth curves for alignment
        if (pathPoints.size() < 2) {
            return false;
        }

        int nearestSegmentIndex = findNearestSegmentIndex(position, pathPoints);
        Point2D segmentStart = pathPoints.get(nearestSegmentIndex);
        Point2D segmentEnd = pathPoints.get(nearestSegmentIndex + 1);

        // Find the exact point on the wire path closest to the click position
        Point2D alignedPosition = findClosestPointOnLineSegment(position, segmentStart, segmentEnd);

        // Allow bend creation even if wire passes over systems
        // This removes the restriction that prevented bend creation on wires crossing systems

        // Insert the bend at the aligned position in the bend list so ordering is preserved
        // Mapping: segment 0 => insert at index 0, last segment => insert at bends.size()
        WireBend bend = new WireBend(alignedPosition, 50.0);
        bends.add(nearestSegmentIndex, bend);
        return true;
    }

    private Point2D findClosestPointOnLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double A = point.getX() - lineStart.getX();
        double B = point.getY() - lineStart.getY();
        double C = lineEnd.getX() - lineStart.getX();
        double D = lineEnd.getY() - lineStart.getY();

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;

        if (lenSq == 0) {
            // Line segment is actually a point
            return lineStart;
        }

        double param = dot / lenSq;

        // Clamp parameter to line segment bounds
        param = Math.max(0.0, Math.min(1.0, param));

        // Calculate the closest point on the line segment
        double x = lineStart.getX() + param * C;
        double y = lineStart.getY() + param * D;

        return new Point2D(x, y);
    }

    @Deprecated
    public boolean addBend(Point2D position) {
        if (bends.size() >= 3) {
            return false; // Maximum 3 bends allowed
        }

        WireBend bend = new WireBend(position, 50.0);
        bends.add(bend);
        return true;
    }

    public boolean moveBend(int bendIndex, Point2D newPosition, List<System> systems) {
        if (bendIndex < 0 || bendIndex >= bends.size()) {
            return false;
        }

        // Temporarily remove the bend to compute adjacency correctly
        WireBend bend = bends.get(bendIndex);
        Point2D originalPosition = bend.getPosition();
        bends.remove(bendIndex);

        // Determine adjacent points (previous and next) for validation
        Point2D prevPoint = (bendIndex == 0) ?
                (sourcePort != null ? sourcePort.getPosition() : originalPosition) :
                bends.get(bendIndex - 1).getPosition();
        Point2D nextPoint = (bendIndex < bends.size()) ?
                bends.get(bendIndex).getPosition() :
                (destinationPort != null ? destinationPort.getPosition() : originalPosition);

        if (prevPoint == null || nextPoint == null) {
            // Restore and fail safely if endpoints are not available
            bends.add(bendIndex, bend);
            return false;
        }

        // Validate movement only against the affected neighboring segments
        if (wouldSegmentWithBendIntersectSystems(prevPoint, newPosition, nextPoint, systems)) {
            // Restore the original bend
            bends.add(bendIndex, bend);
            return false;
        }

        // Move the bend to the new position and restore at the same index
        boolean moveSuccess = bend.moveTo(newPosition, originalPosition);
        bends.add(bendIndex, bend);
        return moveSuccess;
    }

    public boolean moveBendPermissive(int bendIndex, Point2D newPosition, List<System> systems) {
        if (bendIndex < 0 || bendIndex >= bends.size()) {
            return false;
        }

        // Get the bend and its original position
        WireBend bend = bends.get(bendIndex);
        Point2D originalPosition = bend.getPosition();

        // Only check for extreme cases (e.g., bends inside connected systems)
        // Allow more freedom in positioning for better user experience

        // Check if the new position is inside the source or destination system
        if (sourcePort != null && sourcePort.getParentSystem() != null) {
            if (sourcePort.getParentSystem().getBounds().contains(newPosition.getX(), newPosition.getY())) {
                return false; // Don't allow bends inside source system
            }
        }

        if (destinationPort != null && destinationPort.getParentSystem() != null) {
            if (destinationPort.getParentSystem().getBounds().contains(newPosition.getX(), newPosition.getY())) {
                return false; // Don't allow bends inside destination system
            }
        }

        // For better visual alignment, we could optionally snap the bend to the nearest wire path
        // But for maximum user freedom, we'll allow the bend to be positioned anywhere
        // The wire path will automatically adjust to pass through the new bend position

        // Move the bend to the new position
        return bend.moveTo(newPosition, originalPosition);
    }

    @Deprecated
    public boolean moveBend(int bendIndex, Point2D newPosition) {
        if (bendIndex < 0 || bendIndex >= bends.size()) {
            return false;
        }

        WireBend bend = bends.get(bendIndex);
        Point2D originalPosition = bend.getPosition();
        return bend.moveTo(newPosition, originalPosition);
    }

    public double calculateTotalLength() {
        if (sourcePort == null || destinationPort == null) {
            return 0.0;
        }

        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();

        if (bends.isEmpty()) {
            // Straight line
            return sourcePos.distanceTo(destPos);
        }

        // Calculate polyline length
        double totalLength = 0.0;
        Point2D currentPoint = sourcePos;

        for (WireBend bend : bends) {
            totalLength += currentPoint.distanceTo(bend.getPosition());
            currentPoint = bend.getPosition();
        }

        totalLength += currentPoint.distanceTo(destPos);
        return totalLength;
    }

    public boolean wouldBendPassOverSystems(Point2D bendPosition, List<System> systems) {
        if (sourcePort == null || destinationPort == null) {
            return false;
        }

        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();

        // Backward-compatible behavior: validate against full endpoints
        return wouldSegmentWithBendIntersectSystems(sourcePos, bendPosition, destPos, systems);
    }

    private int findNearestSegmentIndex(Point2D position, List<Point2D> pathPoints) {
        int nearestIndex = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            Point2D closestOnSeg = getClosestPointOnSegment(start, end, position);
            double dist = position.distanceTo(closestOnSeg);
            if (dist < minDistance) {
                minDistance = dist;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    private boolean wouldSegmentWithBendIntersectSystems(
            Point2D segmentStart,
            Point2D bendPosition,
            Point2D segmentEnd,
            List<System> systems
    ) {
        for (System system : systems) {
            // Skip systems connected by the endpoints of this wire
            if (sourcePort != null && (system == sourcePort.getParentSystem())) {
                continue;
            }
            if (destinationPort != null && (system == destinationPort.getParentSystem())) {
                continue;
            }

            if (lineIntersectsRectangle(segmentStart, bendPosition, system.getBounds()) ||
                    lineIntersectsRectangle(bendPosition, segmentEnd, system.getBounds())) {
                return true;
            }
        }
        return false;
    }

    public boolean passesOverSystems(List<System> systems) {
        return passesOverSystems(systems, true); // Default to smooth curves
    }

    public boolean passesOverSystems(List<System> systems, boolean useSmoothCurves) {
        if (sourcePort == null || destinationPort == null) {
            return false;
        }

        // Get all path points for the wire (including bends)
        // Use the specified curve mode for accurate collision detection
        List<Point2D> pathPoints = getPathPoints(useSmoothCurves);
        
        // Check each system for intersection
        for (System system : systems) {
            // Check ALL systems, including source and destination systems
            // A wire can pass over its own source/destination system if it has bends

            java.awt.geom.Rectangle2D systemBounds = system.getBounds();

            // Check each segment of the wire path
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point2D segmentStart = pathPoints.get(i);
                Point2D segmentEnd = pathPoints.get(i + 1);
                
                // Check if this segment intersects with system bounds
                if (lineIntersectsRectangle(segmentStart, segmentEnd, systemBounds)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean lineIntersectsRectangle(Point2D lineStart, Point2D lineEnd,
                                            java.awt.geom.Rectangle2D rect) {
        double x1 = lineStart.getX();
        double y1 = lineStart.getY();
        double x2 = lineEnd.getX();
        double y2 = lineEnd.getY();

        double rectX = rect.getX();
        double rectY = rect.getY();
        double rectWidth = rect.getWidth();
        double rectHeight = rect.getHeight();
        double rectRight = rectX + rectWidth;
        double rectBottom = rectY + rectHeight;

        // Check if either endpoint is inside the rectangle
        if (pointInRectangle(x1, y1, rectX, rectY, rectWidth, rectHeight) ||
            pointInRectangle(x2, y2, rectX, rectY, rectWidth, rectHeight)) {
            return true;
        }

        // Check if line segment is completely outside rectangle
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);

        if (maxX < rectX || minX > rectRight || maxY < rectY || minY > rectBottom) {
            return false;
        }

        // Check intersection with each edge of the rectangle
        // Top edge
        if (lineSegmentIntersection(x1, y1, x2, y2, rectX, rectY, rectRight, rectY)) {
            return true;
        }
        // Bottom edge
        if (lineSegmentIntersection(x1, y1, x2, y2, rectX, rectBottom, rectRight, rectBottom)) {
            return true;
        }
        // Left edge
        if (lineSegmentIntersection(x1, y1, x2, y2, rectX, rectY, rectX, rectBottom)) {
            return true;
        }
        // Right edge
        if (lineSegmentIntersection(x1, y1, x2, y2, rectRight, rectY, rectRight, rectBottom)) {
            return true;
        }

        return false;
    }

    private boolean pointInRectangle(double px, double py, double rx, double ry, double rw, double rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    private boolean lineSegmentIntersection(double x1, double y1, double x2, double y2,
                                          double x3, double y3, double x4, double y4) {
        double denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        
        // Lines are parallel
        if (Math.abs(denominator) < 1e-10) {
            return false;
        }

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denominator;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denominator;

        // Check if intersection point is within both line segments
        return t >= 0 && t <= 1 && u >= 0 && u <= 1;
    }

    public boolean incrementBulkPacketPassage() {
        // This method is deprecated - bulk packet counting now happens in acceptPacket()
        // and wire destruction happens when packet exits in transferPackets()
        bulkPacketPassages++;
        return bulkPacketPassages >= MAX_BULK_PASSAGES;
    }
    
    public int getMaxBulkPassages() {
        return MAX_BULK_PASSAGES;
    }

    public List<Point2D> getPathPoints() {
        return getPathPoints(true); // Use smooth curves so packets follow curved wire paths
    }

    public List<Point2D> getPathPoints(boolean useSmoothCurves) {
        if (!useSmoothCurves) {
            // Original rigid polyline behavior
            List<Point2D> points = new ArrayList<>();

            if (sourcePort != null) {
                points.add(sourcePort.getPosition());
            }

            for (WireBend bend : bends) {
                points.add(bend.getPosition());
            }

            if (destinationPort != null) {
                points.add(destinationPort.getPosition());
            }

            return points;
        } else {
            // Generate smooth curved path that ensures bends are always on the path
            return generateSmoothPathPointsWithBendAlignment();
        }
    }

    private List<Point2D> generateSmoothPathPoints() {
        List<Point2D> controlPoints = new ArrayList<>();

        // Add source port as first control point
        if (sourcePort != null) {
            controlPoints.add(sourcePort.getPosition());
        }

        // Add bend points as control points
        for (WireBend bend : bends) {
            controlPoints.add(bend.getPosition());
        }

        // Add destination port as last control point
        if (destinationPort != null) {
            controlPoints.add(destinationPort.getPosition());
        }

        if (controlPoints.size() < 2) {
            return controlPoints; // Need at least 2 points for a path
        }

        // Generate smooth curve with appropriate density
        return generateBezierCurve(controlPoints);
    }

    private List<Point2D> generateSmoothPathPointsWithBendAlignment() {
        if (bends.isEmpty()) {
            // No bends - just return straight line
            List<Point2D> points = new ArrayList<>();
            if (sourcePort != null) {
                points.add(sourcePort.getPosition());
            }
            if (destinationPort != null) {
                points.add(destinationPort.getPosition());
            }
            return points;
        }

        List<Point2D> alignedPath = new ArrayList<>();

        // Start with source port
        if (sourcePort != null) {
            alignedPath.add(sourcePort.getPosition());
        }

        // For each bend, create a smooth curve from the previous point to the bend
        Point2D currentPoint = sourcePort != null ? sourcePort.getPosition() : new Point2D(0, 0);

        for (int i = 0; i < bends.size(); i++) {
            WireBend bend = bends.get(i);
            Point2D bendPos = bend.getPosition();

            // Generate smooth curve from current point to this bend
            List<Point2D> curveSegment = generateSmoothCurveSegment(currentPoint, bendPos);

            // Add all curve points except the last one (to avoid duplication)
            for (int j = 0; j < curveSegment.size() - 1; j++) {
                alignedPath.add(curveSegment.get(j));
            }

            // Add the bend position exactly (ensuring perfect alignment)
            alignedPath.add(bendPos);
            currentPoint = bendPos;
        }

        // Generate final curve to destination port
        if (destinationPort != null) {
            List<Point2D> finalCurve = generateSmoothCurveSegment(currentPoint, destinationPort.getPosition());

            // Add all curve points except the first one (to avoid duplication)
            for (int j = 1; j < finalCurve.size(); j++) {
                alignedPath.add(finalCurve.get(j));
            }
        }

        return alignedPath;
    }

    private List<Point2D> generateSmoothCurveSegment(Point2D start, Point2D end) {
        // Calculate a control point that creates a natural curve
        double distance = start.distanceTo(end);
        double controlDistance = distance * 0.3; // Control point at 30% of distance

        // Create a control point perpendicular to the line, creating a gentle curve
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();

        // Perpendicular vector (rotate 90 degrees)
        double perpX = -dy;
        double perpY = dx;

        // Normalize and scale
        double perpLength = Math.sqrt(perpX * perpX + perpY * perpY);
        if (perpLength > 0) {
            perpX = (perpX / perpLength) * controlDistance;
            perpY = (perpY / perpLength) * controlDistance;
        }

        // Control point at midpoint with perpendicular offset
        double midX = (start.getX() + end.getX()) / 2.0;
        double midY = (start.getY() + end.getY()) / 2.0;
        Point2D controlPoint = new Point2D(midX + perpX, midY + perpY);

        // Generate quadratic Bézier curve
        return generateQuadraticBezierCurve(start, controlPoint, end);
    }

    private List<Point2D> generateBezierCurve(List<Point2D> controlPoints) {
        List<Point2D> curvePoints = new ArrayList<>();

        if (controlPoints.size() == 2) {
            // Straight line - just return the two points
            return controlPoints;
        } else if (controlPoints.size() == 3) {
            // Quadratic Bézier curve (3 control points)
            return generateQuadraticBezierCurve(controlPoints.get(0), controlPoints.get(1), controlPoints.get(2));
        } else {
            // Multiple segments with smooth transitions
            return generateMultiSegmentBezierCurve(controlPoints);
        }
    }

    private List<Point2D> generateQuadraticBezierCurve(Point2D p0, Point2D p1, Point2D p2) {
        List<Point2D> curvePoints = new ArrayList<>();

        // Number of interpolation steps - more steps = smoother curve
        int steps = calculateOptimalSteps(p0, p1, p2);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Point2D point = quadraticBezierInterpolate(p0, p1, p2, t);
            curvePoints.add(point);
        }

        return curvePoints;
    }

    private List<Point2D> generateMultiSegmentBezierCurve(List<Point2D> controlPoints) {
        List<Point2D> curvePoints = new ArrayList<>();

        if (controlPoints.size() < 3) {
            return controlPoints; // Need at least 3 points for multi-segment
        }

        // Start with first point
        curvePoints.add(controlPoints.get(0));

        // For multiple control points, use a more sophisticated approach
        // Generate smooth curve through all points using Catmull-Rom splines
        return generateCatmullRomSpline(controlPoints);
    }

    private List<Point2D> generateCatmullRomSpline(List<Point2D> controlPoints) {
        List<Point2D> curvePoints = new ArrayList<>();

        if (controlPoints.size() < 2) {
            return controlPoints;
        }

        // Add first point
        curvePoints.add(controlPoints.get(0));

        // Generate curve segments between each pair of control points
        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Point2D p0, p1, p2, p3;

            // Handle boundary conditions for smooth start/end
            if (i == 0) {
                // First segment: extrapolate backwards
                p0 = extrapolatePoint(controlPoints.get(1), controlPoints.get(0));
                p1 = controlPoints.get(0);
                p2 = controlPoints.get(1);
                p3 = controlPoints.size() > 2 ? controlPoints.get(2) : extrapolatePoint(controlPoints.get(0), controlPoints.get(1));
            } else if (i == controlPoints.size() - 2) {
                // Last segment: extrapolate forwards
                p0 = controlPoints.get(i - 1);
                p1 = controlPoints.get(i);
                p2 = controlPoints.get(i + 1);
                p3 = extrapolatePoint(controlPoints.get(i), controlPoints.get(i + 1));
            } else {
                // Middle segments: use adjacent points
                p0 = controlPoints.get(i - 1);
                p1 = controlPoints.get(i);
                p2 = controlPoints.get(i + 1);
                p3 = controlPoints.get(i + 2);
            }

            // Generate smooth curve segment using Catmull-Rom interpolation
            List<Point2D> segment = generateCatmullRomSegment(p0, p1, p2, p3);

            // Add segment points (skip first to avoid duplication)
            for (int j = 1; j < segment.size(); j++) {
                curvePoints.add(segment.get(j));
            }
        }

        return curvePoints;
    }

    private List<Point2D> generateCatmullRomSegment(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        List<Point2D> segment = new ArrayList<>();

        // Calculate optimal number of steps based on segment length and complexity
        double segmentLength = p1.distanceTo(p2);
        int steps = Math.max(15, (int) (segmentLength / 5.0)); // At least 15 steps, more for longer segments

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Point2D point = catmullRomInterpolate(p0, p1, p2, p3, t);
            segment.add(point);
        }

        return segment;
    }

    private Point2D catmullRomInterpolate(Point2D p0, Point2D p1, Point2D p2, Point2D p3, double t) {
        // Catmull-Rom matrix coefficients
        double t2 = t * t;
        double t3 = t2 * t;

        // Catmull-Rom blending functions
        double b0 = -0.5 * t3 + t2 - 0.5 * t;
        double b1 = 1.5 * t3 - 2.5 * t2 + 1.0;
        double b2 = -1.5 * t3 + 2.0 * t2 + 0.5 * t;
        double b3 = 0.5 * t3 - 0.5 * t2;

        // Interpolate x and y coordinates
        double x = b0 * p0.getX() + b1 * p1.getX() + b2 * p2.getX() + b3 * p3.getX();
        double y = b0 * p0.getY() + b1 * p1.getY() + b2 * p2.getY() + b3 * p3.getY();

        return new Point2D(x, y);
    }

    private Point2D extrapolatePoint(Point2D p1, Point2D p2) {
        // Simple linear extrapolation: extend the line from p1 to p2
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();

        return new Point2D(p1.getX() - dx, p1.getY() - dy);
    }

    private int calculateOptimalSteps(Point2D p0, Point2D p1, Point2D p2) {
        // Calculate curve complexity based on control point deviation
        double straightLineLength = p0.distanceTo(p2);
        double actualPathLength = p0.distanceTo(p1) + p1.distanceTo(p2);
        double deviation = actualPathLength - straightLineLength;

        // More deviation = more complex curve = more interpolation steps
        int baseSteps = 20; // Base smoothness
        int additionalSteps = (int) Math.min(30, deviation / 10.0); // Cap at reasonable limit

        return Math.max(10, baseSteps + additionalSteps); // Minimum 10 steps for smoothness
    }

    private Point2D quadraticBezierInterpolate(Point2D p0, Point2D p1, Point2D p2, double t) {
        // Quadratic Bézier formula: B(t) = (1-t)²P₀ + 2(1-t)tP₁ + t²P₂
        double oneMinusT = 1.0 - t;
        double oneMinusTSquared = oneMinusT * oneMinusT;
        double tSquared = t * t;

        double x = oneMinusTSquared * p0.getX() + 2 * oneMinusT * t * p1.getX() + tSquared * p2.getX();
        double y = oneMinusTSquared * p0.getY() + 2 * oneMinusT * t * p1.getY() + tSquared * p2.getY();

        return new Point2D(x, y);
    }

    public double getTotalLength() {
        return getTotalLength(true); // Default to smooth curves for backward compatibility
    }

    public double getTotalLength(boolean useSmoothCurves) {
        List<Point2D> pathPoints = getPathPoints(useSmoothCurves);
        if (pathPoints.size() < 2) {
            return wireLength; // Fallback to stored length
        }

        double totalLength = 0.0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            totalLength += start.distanceTo(end);
        }

        return totalLength;
    }

    public Point2D getPositionAtProgress(double progress) {
        return getPositionAtProgress(progress, true); // Default to smooth curves for backward compatibility
    }

    public Point2D getPositionAtProgress(double progress, boolean useSmoothCurves) {
        List<Point2D> pathPoints = getPathPoints(useSmoothCurves);
        if (pathPoints.size() < 2) {
            return pathPoints.isEmpty() ? new Point2D(0, 0) : pathPoints.get(0);
        }

        // Clamp progress to valid range
        progress = Math.max(0.0, Math.min(1.0, progress));

        // Calculate target distance along the path
        double totalLength = getTotalLength(useSmoothCurves);
        double targetDistance = progress * totalLength;

        // Find the segment and interpolate within it
        double accumulatedDistance = 0.0;
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);
            double segmentLength = start.distanceTo(end);

            if (accumulatedDistance + segmentLength >= targetDistance) {
                // The target position is within this segment
                double segmentProgress = (targetDistance - accumulatedDistance) / segmentLength;
                return interpolatePosition(start, end, segmentProgress);
            }

            accumulatedDistance += segmentLength;
        }

        // If we reach here, return the destination port position
        return pathPoints.get(pathPoints.size() - 1);
    }

    private Point2D interpolatePosition(Point2D start, Point2D end, double progress) {
        double x = start.getX() + (end.getX() - start.getX()) * progress;
        double y = start.getY() + (end.getY() - start.getY()) * progress;
        return new Point2D(x, y);
    }



    public void clearAllPackets() {
        packetsOnWire.clear();
    }

    public Point2D getClosestPointOnWire(Point2D targetPoint) {
        List<Point2D> pathPoints = getPathPoints();
        if (pathPoints.isEmpty()) {
            return null;
        }

        Point2D closestPoint = pathPoints.get(0);
        double closestDistance = targetPoint.distanceTo(closestPoint);

        // Check all path points and line segments
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);

            // Find closest point on line segment
            Point2D segmentClosest = getClosestPointOnSegment(start, end, targetPoint);
            double distance = targetPoint.distanceTo(segmentClosest);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestPoint = segmentClosest;
            }
        }

        return closestPoint;
    }

    public double getProgressAtPoint(Point2D point) {
        List<Point2D> pathPoints = getPathPoints();
        if (pathPoints.isEmpty()) {
            return 0.0;
        }

        double totalLength = getTotalLength();
        if (totalLength == 0) {
            return 0.0;
        }

        double accumulatedLength = 0.0;
        Point2D closestPoint = getClosestPointOnWire(point);

        // Find which segment contains the closest point and calculate progress
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point2D start = pathPoints.get(i);
            Point2D end = pathPoints.get(i + 1);

            Point2D segmentClosest = getClosestPointOnSegment(start, end, point);

            if (segmentClosest.distanceTo(closestPoint) < 1.0) { // Found the segment
                double segmentProgress = start.distanceTo(segmentClosest) / start.distanceTo(end);
                double segmentLength = start.distanceTo(end);
                return (accumulatedLength + segmentProgress * segmentLength) / totalLength;
            }

            accumulatedLength += start.distanceTo(end);
        }

        return 1.0; // Default to end if not found
    }

    public double getDistanceToPoint(Point2D targetPoint) {
        Point2D closestPoint = getClosestPointOnWire(targetPoint);
        return closestPoint != null ? targetPoint.distanceTo(closestPoint) : Double.MAX_VALUE;
    }

    private Point2D getClosestPointOnSegment(Point2D start, Point2D end, Point2D target) {
        double segmentLength = start.distanceTo(end);
        if (segmentLength == 0) {
            return start;
        }

        Vec2D segmentVector = new Vec2D(end.getX() - start.getX(), end.getY() - start.getY());
        Vec2D targetVector = new Vec2D(target.getX() - start.getX(), target.getY() - start.getY());

        double projection = targetVector.dot(segmentVector) / (segmentLength * segmentLength);
        projection = Math.max(0.0, Math.min(1.0, projection)); // Clamp to segment

        return new Point2D(
                start.getX() + projection * segmentVector.getX(),
                start.getY() + projection * segmentVector.getY()
        );
    }

    public void updatePortReferences(Port newSourcePort, Port newDestinationPort) {
        // Normalize direction: ensure source is OUTPUT and destination is INPUT
        Port normalizedSource = newSourcePort;
        Port normalizedDestination = newDestinationPort;
        if (newSourcePort != null && newDestinationPort != null) {
            boolean srcIsInput = newSourcePort.isInput();
            boolean dstIsInput = newDestinationPort.isInput();
            if (srcIsInput && !dstIsInput) {
                // Swap so that source is always an output and destination is input
                normalizedSource = newDestinationPort;
                normalizedDestination = newSourcePort;
            }
        }
        this.sourcePort = normalizedSource;
        this.destinationPort = normalizedDestination;

        // Recalculate path points with new port positions
        calculatePathPoints();
    }

    private void calculatePathPoints() {
        // For now, use a simple straight line path
        // This could be enhanced later to handle bends properly
        if (sourcePort != null && destinationPort != null) {
            Point2D sourcePos = sourcePort.getPosition();
            Point2D destPos = destinationPort.getPosition();

            if (sourcePos != null && destPos != null) {
                // Update wire length if needed
                this.wireLength = sourcePos.distanceTo(destPos);
            }
        }
    }

    // Owner player ID methods for multiplayer games
    public String getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public void setOwnerPlayerId(String ownerPlayerId) {
        this.ownerPlayerId = ownerPlayerId;
    }

}
