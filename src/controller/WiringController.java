package controller;

import model.*;
import java.util.*;

public class WiringController {

    public WiringController() {
    }

    public boolean createWireConnection(Port sourcePort, Port destinationPort, GameState gameState) {

        // Check if connection is valid
        if (!isValidConnection(sourcePort, destinationPort, gameState)) {
            
            return false;
        }

        // Calculate wire length
        double wireLength = calculateWireLength(sourcePort, destinationPort);

        // Check if enough wire length is available
        if (wireLength > gameState.getRemainingWireLength()) {
            
            return false;
        }

        // Create wire connection
        WireConnection connection = new WireConnection(sourcePort, destinationPort, wireLength);

        gameState.addWireConnection(connection);

        // Update port connection status
        sourcePort.setConnected(true);
        destinationPort.setConnected(true);

        // Immediately update system indicators for both connected systems
        sourcePort.getParentSystem().update(0.0); // Force indicator update
        destinationPort.getParentSystem().update(0.0); // Force indicator update

        // Update remaining wire length
        gameState.setRemainingWireLength(gameState.getRemainingWireLength() - wireLength);

        return true;
    }

    private boolean isValidConnection(Port sourcePort, Port destinationPort, GameState gameState) {

        // Check if ports are from the same system
        if (sourcePort.getParentSystem() == destinationPort.getParentSystem()) {
            
            return false;
        }

        // Port shape compatibility check - all shapes can now connect
        if (!sourcePort.getShape().isCompatibleWith(destinationPort.getShape())) {
            
            return false;
        }

        // Check if one is input and one is output (prevent output-to-output connections)
        if (sourcePort.isInput() == destinationPort.isInput()) {
            
            return false;
        }

        // Check if connection already exists
        if (gameState.hasWireConnection(sourcePort, destinationPort)) {
            
            return false;
        }

        // Check if EITHER port is already connected (prevent multiple connections per port)
        if (sourcePort.isConnected() || destinationPort.isConnected()) {
            
            return false;
        }

        // Check if wire would pass over any systems (Phase 2 requirement)
        WireConnection tempConnection = new WireConnection(sourcePort, destinationPort);
        if (tempConnection.passesOverSystems(gameState.getCurrentLevel().getSystems())) {
            
            return false;
        }

        return true;
    }

    private double calculateWireLength(Port sourcePort, Port destinationPort) {
        Point2D sourcePos = sourcePort.getPosition();
        Point2D destPos = destinationPort.getPosition();
        return sourcePos.distanceTo(destPos);
    }

    private double calculateWireLengthWithBends(WireConnection connection) {
        return connection.getTotalLength();
    }

    public boolean isNetworkConnected(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        if (systems.isEmpty()) {
            return false;
        }

        // Use depth-first search to check connectivity
        Set<String> visited = new HashSet<>();
        dfs(systems.get(0), visited, gameState);

        // Check if all systems are reachable
        return visited.size() == systems.size();
    }

    public int getReachableSystemCount(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        if (systems.isEmpty()) {
            return 0;
        }

        Set<String> visited = new HashSet<>();
        dfs(systems.get(0), visited, gameState);
        return visited.size();
    }

    public boolean areAllPortsConnected(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        if (systems.isEmpty()) {
            
            return false;
        }

        for (model.System system : systems) {
            // Skip systems that don't have any ports (like HUD display elements)
            List<Port> allPorts = system.getAllPorts();
            if (allPorts.isEmpty()) {
                
                continue;
            }

            for (Port port : allPorts) {
                
                if (!port.isConnected()) {
                    
                    return false; // Found an unconnected port
                }
            }
        }

        return true; // All ports are connected
    }

    public int[] getPortConnectivityCounts(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        int totalPorts = 0;
        int connectedPorts = 0;

        for (model.System system : systems) {
            // Skip systems that don't have any ports (like HUD display elements)
            List<Port> allPorts = system.getAllPorts();
            if (allPorts.isEmpty()) {
                continue;
            }

            totalPorts += allPorts.size();
            for (Port port : allPorts) {
                if (port.isConnected()) {
                    connectedPorts++;
                }
            }
        }

        return new int[]{connectedPorts, totalPorts};
    }

    public List<Port> getUnconnectedPorts(GameState gameState) {
        List<Port> unconnectedPorts = new ArrayList<>();
        List<model.System> systems = gameState.getCurrentLevel().getSystems();

        for (model.System system : systems) {
            // Skip systems that don't have any ports (like HUD display elements)
            List<Port> allPorts = system.getAllPorts();
            if (allPorts.isEmpty()) {
                continue;
            }

            for (Port port : allPorts) {
                if (!port.isConnected()) {
                    unconnectedPorts.add(port);
                }
            }
        }

        return unconnectedPorts;
    }

    public LevelValidationResult validateLevelDesign(GameState gameState) {
        List<model.System> systems = gameState.getCurrentLevel().getSystems();
        int totalInputPorts = 0;
        int totalOutputPorts = 0;
        Map<PortShape, Integer> inputPortShapes = new HashMap<>();
        Map<PortShape, Integer> outputPortShapes = new HashMap<>();

        for (model.System system : systems) {
            List<Port> allPorts = system.getAllPorts();
            if (allPorts.isEmpty()) {
                continue;
            }

            for (Port port : allPorts) {
                if (port.isInput()) {
                    totalInputPorts++;
                    inputPortShapes.merge(port.getShape(), 1, Integer::sum);
                } else {
                    totalOutputPorts++;
                    outputPortShapes.merge(port.getShape(), 1, Integer::sum);
                }
            }
        }

        // Check if total port counts are balanced
        boolean balancedPorts = totalInputPorts == totalOutputPorts;

        // Check if port shapes are compatible
        boolean compatibleShapes = true;
        StringBuilder shapeIssues = new StringBuilder();

        for (PortShape shape : PortShape.values()) {
            int inputCount = inputPortShapes.getOrDefault(shape, 0);
            int outputCount = outputPortShapes.getOrDefault(shape, 0);

            if (inputCount != outputCount) {
                compatibleShapes = false;
                shapeIssues.append(String.format(" %s: %d input vs %d output,",
                        shape, inputCount, outputCount));
            }
        }

        return new LevelValidationResult(
                balancedPorts,
                compatibleShapes,
                totalInputPorts,
                totalOutputPorts,
                inputPortShapes,
                outputPortShapes,
                shapeIssues.toString()
        );
    }

    private void dfs(model.System system, Set<String> visited, GameState gameState) {
        visited.add(system.getId());

        // Get all connected systems
        for (WireConnection connection : gameState.getWireConnections()) {
            if (connection.isActive()) {
                model.System connectedSystem = null;

                if (connection.getSourcePort().getParentSystem().getId().equals(system.getId())) {
                    connectedSystem = connection.getDestinationPort().getParentSystem();
                } else if (connection.getDestinationPort().getParentSystem().getId().equals(system.getId())) {
                    connectedSystem = connection.getSourcePort().getParentSystem();
                }

                if (connectedSystem != null && !visited.contains(connectedSystem.getId())) {
                    dfs(connectedSystem, visited, gameState);
                }
            }
        }
    }

    public boolean willCreateConnectedGraph(Port sourcePort, Port destinationPort, GameState gameState) {
        // Temporarily add the connection
        WireConnection tempConnection = new WireConnection(sourcePort, destinationPort, 0.0);
        gameState.addWireConnection(tempConnection);

        // Check if network is connected
        boolean isConnected = isNetworkConnected(gameState);

        // Remove temporary connection
        gameState.removeWireConnection(tempConnection);

        return isConnected;
    }

    public boolean addBendToWire(WireConnection connection, Point2D bendPosition, GameState gameState) {
        return addBendToWire(connection, bendPosition, gameState, true); // Default to smooth curves for backward compatibility
    }

    public boolean addBendToWire(WireConnection connection, Point2D bendPosition, GameState gameState, boolean useSmoothCurves) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Check if player has enough coins (1 coin per bend)
        if (gameState.getCoins() < 1) {
            return false;
        }

        // Calculate the wire length before adding the bend using the correct curve mode
        double lengthBeforeBend = connection.getTotalLength(useSmoothCurves);

        // Try to add the bend
        if (connection.addBend(bendPosition, gameState.getCurrentLevel().getSystems())) {
            // Calculate the new wire length after adding the bend using the correct curve mode
            double lengthAfterBend = connection.getTotalLength(useSmoothCurves);
            double lengthIncrease = lengthAfterBend - lengthBeforeBend;

            // Check if there's enough remaining wire length for the bend
            if (lengthIncrease > gameState.getRemainingWireLength()) {
                // Not enough wire length, remove the bend from the list and fail
                List<WireBend> bends = connection.getBends();
                if (!bends.isEmpty()) {
                    bends.remove(bends.size() - 1);
                }
                return false;
            }

            // Deduct the additional wire length from the available pool
            gameState.setRemainingWireLength(gameState.getRemainingWireLength() - lengthIncrease);

            // Deduct coin cost
            gameState.spendCoins(1);
            return true;
        }

        return false;
    }

    public boolean moveBendOnWire(WireConnection connection, int bendIndex, Point2D newPosition, GameState gameState) {
        return moveBendOnWire(connection, bendIndex, newPosition, gameState, true); // Default to smooth curves for backward compatibility
    }

    public boolean moveBendOnWire(WireConnection connection, int bendIndex, Point2D newPosition, GameState gameState, boolean useSmoothCurves) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Calculate the wire length before moving the bend using the correct curve mode
        double lengthBeforeMove = connection.getTotalLength(useSmoothCurves);

        // Try to move the bend with more permissive validation for better user experience
        if (connection.moveBendPermissive(bendIndex, newPosition, gameState.getCurrentLevel().getSystems())) {
            // Calculate the new wire length after moving the bend using the correct curve mode
            double lengthAfterMove = connection.getTotalLength(useSmoothCurves);
            double lengthChange = lengthAfterMove - lengthBeforeMove;

            // Update the remaining wire length based on the change
            if (lengthChange > 0) {
                // Wire became longer, need to deduct additional length
                if (lengthChange > gameState.getRemainingWireLength()) {
                    // Not enough wire length, revert the move and fail
                    // We need to restore the original position
                    Point2D originalPosition = connection.getBends().get(bendIndex).getPosition();
                    connection.moveBend(bendIndex, originalPosition, gameState.getCurrentLevel().getSystems());
                    return false;
                }
                gameState.setRemainingWireLength(gameState.getRemainingWireLength() - lengthChange);
            } else if (lengthChange < 0) {
                // Wire became shorter, add the saved length back to the pool
                gameState.setRemainingWireLength(gameState.getRemainingWireLength() - lengthChange);
            }

            return true;
        }

        return false;
    }

    public boolean moveBendFreely(WireConnection connection, int bendIndex, Point2D newPosition, GameState gameState) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Get smooth curve setting to calculate wire length correctly
        boolean useSmoothCurves = true; // Default to smooth curves
        if (gameState != null) {
            Object setting = gameState.getGameSettings().get("smoothWireCurves");
            if (setting instanceof Boolean) {
                useSmoothCurves = (Boolean) setting;
            }
        }

        // Use moveBendOnWire with proper wire length management
        return moveBendOnWire(connection, bendIndex, newPosition, gameState, useSmoothCurves);
    }

    public boolean removeWireConnection(WireConnection connection, GameState gameState) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Get current curve setting
        boolean useSmoothCurves = true; // Default to smooth curves
        Object setting = gameState.getGameSettings().get("smoothWireCurves");
        if (setting instanceof Boolean) {
            useSmoothCurves = (Boolean) setting;
        }

        // Get the total length of the wire including bends (using current curve setting)
        double wireLength = connection.getTotalLength(useSmoothCurves);
        double storedWireLength = connection.getWireLength(); // Original stored length

        // Disconnect the ports
        Port sourcePort = connection.getSourcePort();
        Port destinationPort = connection.getDestinationPort();

        if (sourcePort != null) {
            sourcePort.setConnected(false);
        }
        if (destinationPort != null) {
            destinationPort.setConnected(false);
        }

        // Immediately update system indicators for both disconnected systems
        if (sourcePort != null) {
            sourcePort.getParentSystem().update(0.0); // Force indicator update
        }
        if (destinationPort != null) {
            destinationPort.getParentSystem().update(0.0); // Force indicator update
        }

        // Remove the connection from the game state
        gameState.removeWireConnection(connection);

        // Restore the wire length to available pool (use the actual calculated length with current curve mode)
        double currentLength = gameState.getRemainingWireLength();
        double lengthToRestore = wireLength; // Use calculated length to match what was actually consumed
        gameState.setRemainingWireLength(currentLength + lengthToRestore);

        // Deactivate the connection
        connection.setActive(false);

        return true;
    }

    public boolean mergeWireConnections(WireConnection wire1, WireConnection wire2, GameState gameState) {
        if (wire1 == null || wire2 == null || !wire1.isActive() || !wire2.isActive()) {
            return false;
        }

        // Find the common system/port between the two wires
        Port commonPort = findCommonPort(wire1, wire2);
        if (commonPort == null) {
            return false; // No common connection point
        }

        // Determine the new connection endpoints
        Port newSourcePort = getOtherPort(wire1, commonPort);
        Port newDestinationPort = getOtherPort(wire2, commonPort);

        if (newSourcePort == null || newDestinationPort == null) {
            return false;
        }

        // Check if the new connection would be valid
        if (!isValidConnection(newSourcePort, newDestinationPort, gameState)) {
            return false;
        }

        // Calculate total length needed for the merged wire
        double totalLength = wire1.getTotalLength() + wire2.getTotalLength();

        // Create the new merged connection
        WireConnection mergedConnection = new WireConnection(newSourcePort, newDestinationPort, totalLength);

        // Remove the old connections (without restoring length since we're using it)
        wire1.setActive(false);
        wire2.setActive(false);
        gameState.removeWireConnection(wire1);
        gameState.removeWireConnection(wire2);

        // Add the new merged connection
        gameState.addWireConnection(mergedConnection);

        // Update port connections
        newSourcePort.setConnected(true);
        newDestinationPort.setConnected(true);
        commonPort.setConnected(false); // Common port is no longer connected

        // Immediately update system indicators for all affected systems
        newSourcePort.getParentSystem().update(0.0); // Force indicator update
        newDestinationPort.getParentSystem().update(0.0); // Force indicator update
        commonPort.getParentSystem().update(0.0); // Force indicator update

        return true;
    }

    private Port findCommonPort(WireConnection wire1, WireConnection wire2) {
        if (wire1.getSourcePort() == wire2.getSourcePort() ||
                wire1.getSourcePort() == wire2.getDestinationPort()) {
            return wire1.getSourcePort();
        }
        if (wire1.getDestinationPort() == wire2.getSourcePort() ||
                wire1.getDestinationPort() == wire2.getDestinationPort()) {
            return wire1.getDestinationPort();
        }
        return null;
    }

    private Port getOtherPort(WireConnection connection, Port excludePort) {
        if (connection.getSourcePort() == excludePort) {
            return connection.getDestinationPort();
        } else if (connection.getDestinationPort() == excludePort) {
            return connection.getSourcePort();
        }
        return null;
    }

    public boolean canMoveSystem(model.System system, Point2D newPosition, GameState gameState) {
        if (system instanceof ReferenceSystem) {
            return false; // Cannot move reference systems
        }

        Point2D originalPosition = system.getPosition();
        List<model.System> otherSystems = new ArrayList<>(gameState.getCurrentLevel().getSystems());
        otherSystems.remove(system);

        // Temporarily move the system to check constraints
        system.setPosition(newPosition);

        // Update all connected ports' positions
        for (Port port : system.getInputPorts()) {
            port.updatePositionRelativeToSystem();
        }
        for (Port port : system.getOutputPorts()) {
            port.updatePositionRelativeToSystem();
        }

        boolean canMove = true;

        // Check if any connected wires would exceed wire length budget
        double totalWireLengthChange = 0.0;
        for (WireConnection connection : gameState.getWireConnections()) {
            if (connection.isActive() &&
                    (connection.getSourcePort().getParentSystem() == system ||
                            connection.getDestinationPort().getParentSystem() == system)) {

                double oldLength = connection.getWireLength();
                double newLength = connection.getTotalLength(); // Recalculated with new position
                totalWireLengthChange += (newLength - oldLength);

                // Check if any wire would pass over other systems
                if (connection.passesOverSystems(otherSystems)) {
                    canMove = false;
                    break;
                }
            }
        }

        // Check if total wire length change would exceed available budget
        if (canMove && totalWireLengthChange > gameState.getRemainingWireLength()) {
            canMove = false;
        }

        // Restore original position if move is not valid
        if (!canMove) {
            system.setPosition(originalPosition);
            for (Port port : system.getInputPorts()) {
                port.updatePositionRelativeToSystem();
            }
            for (Port port : system.getOutputPorts()) {
                port.updatePositionRelativeToSystem();
            }
        }

        return canMove;
    }

    public boolean moveSystem(model.System system, Point2D newPosition, GameState gameState) {
        if (!canMoveSystem(system, newPosition, gameState)) {
            return false;
        }

        // Calculate wire length changes before moving
        double totalWireLengthChange = 0.0;
        List<WireConnection> affectedConnections = new ArrayList<>();

        for (WireConnection connection : gameState.getWireConnections()) {
            if (connection.isActive() &&
                    (connection.getSourcePort().getParentSystem() == system ||
                            connection.getDestinationPort().getParentSystem() == system)) {

                double oldLength = connection.getWireLength();
                affectedConnections.add(connection);
                totalWireLengthChange += (connection.getTotalLength() - oldLength);
            }
        }

        // Move the system
        system.setPosition(newPosition);

        // Update port positions
        for (Port port : system.getInputPorts()) {
            port.updatePositionRelativeToSystem();
        }
        for (Port port : system.getOutputPorts()) {
            port.updatePositionRelativeToSystem();
        }

        // Update wire lengths and consume additional wire length
        for (WireConnection connection : affectedConnections) {
            double newLength = connection.getTotalLength();
            connection.setWireLength(newLength);
        }

        // Deduct the additional wire length from available budget
        if (totalWireLengthChange > 0) {
            gameState.setRemainingWireLength(gameState.getRemainingWireLength() - totalWireLengthChange);
        } else {
            // If wires became shorter, add the saved length back
            gameState.setRemainingWireLength(gameState.getRemainingWireLength() - totalWireLengthChange);
        }

        return true;
    }

    public double getTotalWireLengthUsed(GameState gameState) {
        return getTotalWireLengthUsed(gameState, true); // Default to smooth curves for backward compatibility
    }

    public double getTotalWireLengthUsed(GameState gameState, boolean useSmoothCurves) {
        double totalUsed = 0.0;
        for (WireConnection connection : gameState.getWireConnections()) {
            if (connection.isActive()) {
                double connectionLength = connection.getTotalLength(useSmoothCurves);
                totalUsed += connectionLength;
                
            }
        }
        
        return totalUsed;
    }

    public double getTotalWireLengthAvailable(GameState gameState) {
        return gameState.getRemainingWireLength() + getTotalWireLengthUsed(gameState);
    }

    public double getTotalWireLengthAvailable(GameState gameState, boolean useSmoothCurves) {
        return gameState.getRemainingWireLength() + getTotalWireLengthUsed(gameState, useSmoothCurves);
    }

    public boolean canAddBend(WireConnection connection, Point2D bendPosition, GameState gameState) {
        return canAddBend(connection, bendPosition, gameState, true); // Default to smooth curves for backward compatibility
    }

    public boolean canAddBend(WireConnection connection, Point2D bendPosition, GameState gameState, boolean useSmoothCurves) {
        if (connection == null || !connection.isActive()) {
            return false;
        }

        // Calculate the wire length before adding the bend using the correct curve mode
        double lengthBeforeBend = connection.getTotalLength(useSmoothCurves);

        // Temporarily add the bend to calculate the new length
        List<WireBend> originalBends = new ArrayList<>(connection.getBends());
        WireBend tempBend = new WireBend(bendPosition, 50.0);
        connection.getBends().add(tempBend);

        // Calculate the new wire length after adding the bend using the correct curve mode
        double lengthAfterBend = connection.getTotalLength(useSmoothCurves);
        double lengthIncrease = lengthAfterBend - lengthBeforeBend;

        // Remove the temporary bend
        connection.getBends().remove(tempBend);

        // Check if there's enough remaining wire length for the bend
        return lengthIncrease <= gameState.getRemainingWireLength();
    }

    public boolean canMoveBend(WireConnection connection, int bendIndex, Point2D newPosition, GameState gameState) {
        return canMoveBend(connection, bendIndex, newPosition, gameState, true); // Default to smooth curves for backward compatibility
    }

    public boolean canMoveBend(WireConnection connection, int bendIndex, Point2D newPosition, GameState gameState, boolean useSmoothCurves) {
        if (connection == null || !connection.isActive() || bendIndex < 0 || bendIndex >= connection.getBends().size()) {
            return false;
        }

        // Calculate the wire length before moving the bend using the correct curve mode
        double lengthBeforeMove = connection.getTotalLength(useSmoothCurves);

        // Temporarily move the bend to calculate the new length
        WireBend bend = connection.getBends().get(bendIndex);
        Point2D originalPosition = bend.getPosition();
        bend.moveTo(newPosition, originalPosition);

        // Calculate the new wire length after moving the bend using the correct curve mode
        double lengthAfterMove = connection.getTotalLength(useSmoothCurves);
        double lengthChange = lengthAfterMove - lengthBeforeMove;

        // Restore the original position
        bend.moveTo(originalPosition, newPosition);

        // If the wire would become longer, check if there's enough wire length available
        if (lengthChange > 0) {
            return lengthChange <= gameState.getRemainingWireLength();
        }

        // If the wire would become shorter or stay the same, it's always allowed
        return true;
    }
}

