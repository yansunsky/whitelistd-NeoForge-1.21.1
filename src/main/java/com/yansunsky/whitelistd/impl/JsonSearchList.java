package com.yansunsky.whitelistd.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.yansunsky.whitelistd.PlayerInfo;
import com.yansunsky.whitelistd.SearchList;
import com.yansunsky.whitelistd.SearchMode;
import com.yansunsky.whitelistd.Whitelistd;
import com.yansunsky.whitelistd.WhitelistdRuntimeException;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 使用 JSON 文件存储白名单数据的搜索列表实现。
 */
public class JsonSearchList implements SearchList {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final UUID ZERO_UUID = new UUID(0L, 0L);

    private SearchMode mode = SearchMode.PLAYER_NAME_OR_UUID;
    private boolean playerNameCaseSensitive = true;
    private Path jsonFilePath;

    private final Map<UUID, String> uuidToName = new HashMap<>();
    private final Map<String, UUID> nameToUuid = new HashMap<>();
    private final Set<String> namesWithoutUuid = new HashSet<>();

    @Override
    public void init(SearchMode mode, boolean playerNameCaseSensitive, String[] args) {
        this.mode = Objects.requireNonNullElse(mode, SearchMode.PLAYER_NAME_OR_UUID);
        this.playerNameCaseSensitive = playerNameCaseSensitive;
        if (args == null || args.length < 1 || args[0] == null || args[0].isBlank()) {
            throw new WhitelistdRuntimeException("JSON storage requires a whitelist file path argument");
        }

        Path specificPath = Path.of(args[0]);
        jsonFilePath = specificPath.isAbsolute() ? specificPath : Whitelistd.getInstance().getConfigDir().resolve(specificPath);

        try {
            Path parent = jsonFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(jsonFilePath)) {
                write();
                return;
            }
            read();
            write();
        } catch (IOException e) {
            throw new WhitelistdRuntimeException("Failed to read/write whitelist JSON file", e);
        }
    }

    @Override
    public AddItemState addItem(PlayerInfo player) {
        Objects.requireNonNull(player, "player");
        String name = normalizeName(player.getName());
        UUID uuid = player.getUuid();

        String previousUuidName = uuid == null ? null : uuidToName.get(uuid);
        UUID previousNameUuid = nameToUuid.get(name);
        boolean hadNameWithoutUuid = namesWithoutUuid.contains(name);

        if (uuid == null) {
            if (hadNameWithoutUuid || previousNameUuid != null) {
                return AddItemState.DUPLICATE;
            }
            namesWithoutUuid.add(name);
        } else {
            if (previousUuidName != null || previousNameUuid != null) {
                return AddItemState.DUPLICATE;
            }
            namesWithoutUuid.remove(name);
            uuidToName.put(uuid, name);
            nameToUuid.put(name, uuid);
        }

        try {
            write();
        } catch (IOException e) {
            restoreAdd(name, uuid, hadNameWithoutUuid);
            return AddItemState.IO_ERROR;
        }
        return AddItemState.SUCCESSFUL;
    }

    @Override
    public RemoveItemState removeItem(PlayerInfo player) {
        Objects.requireNonNull(player, "player");
        String name = normalizeName(player.getName());
        UUID uuid = player.getUuid();

        boolean removedNameWithoutUuid = false;
        UUID removedUuid = null;
        String removedUuidName = null;

        if (uuid == null) {
            removedNameWithoutUuid = namesWithoutUuid.remove(name);
            removedUuid = nameToUuid.remove(name);
            if (removedUuid != null) {
                removedUuidName = uuidToName.remove(removedUuid);
            }
        } else {
            removedUuidName = uuidToName.remove(uuid);
            if (removedUuidName != null) {
                nameToUuid.remove(removedUuidName);
                removedUuid = uuid;
            }
        }

        if (!removedNameWithoutUuid && removedUuidName == null) {
            return RemoveItemState.NOT_FOUND;
        }

        try {
            write();
        } catch (IOException e) {
            if (removedNameWithoutUuid) {
                namesWithoutUuid.add(name);
            }
            if (removedUuid != null && removedUuidName != null) {
                uuidToName.put(removedUuid, removedUuidName);
                nameToUuid.put(removedUuidName, removedUuid);
            }
            return RemoveItemState.IO_ERROR;
        }
        return RemoveItemState.SUCCESSFUL;
    }

    @Override
    public QueryResult query(PlayerInfo player) {
        Objects.requireNonNull(player, "player");
        String name = normalizeName(player.getName());
        UUID uuid = player.getUuid();
        boolean found = false;

        switch (mode) {
            case PLAYER_NAME -> {
                UUID storedUuid = findByName(name);
                if (storedUuid != null) {
                    found = true;
                    uuid = storedUuid == ZERO_UUID ? null : storedUuid;
                }
            }
            case PLAYER_UUID -> {
                if (uuid != null) {
                    String storedName = findByUuid(uuid);
                    if (storedName != null) {
                        found = true;
                        name = storedName;
                    }
                }
            }
            case PLAYER_NAME_OR_UUID -> {
                if (uuid != null) {
                    String storedName = findByUuid(uuid);
                    if (storedName != null) {
                        found = true;
                        name = storedName;
                        break;
                    }
                }
                UUID storedUuid = findByName(name);
                if (storedUuid != null) {
                    found = true;
                    uuid = storedUuid == ZERO_UUID ? null : storedUuid;
                }
            }
        }
        return new QueryResult(found, new PlayerInfo(name, uuid));
    }

    @Override
    public ClearState clear() {
        Map<UUID, String> oldUuidToName = new HashMap<>(uuidToName);
        Map<String, UUID> oldNameToUuid = new HashMap<>(nameToUuid);
        Set<String> oldNamesWithoutUuid = new HashSet<>(namesWithoutUuid);

        uuidToName.clear();
        nameToUuid.clear();
        namesWithoutUuid.clear();
        try {
            write();
        } catch (IOException e) {
            uuidToName.putAll(oldUuidToName);
            nameToUuid.putAll(oldNameToUuid);
            namesWithoutUuid.addAll(oldNamesWithoutUuid);
            return ClearState.IO_ERROR;
        }
        return ClearState.SUCCESSFUL;
    }

    @Override
    public int size() {
        return uuidToName.size() + namesWithoutUuid.size();
    }

    @Override
    public Iterable<PlayerInfo> getItems() {
        List<PlayerInfo> items = new ArrayList<>(size());
        namesWithoutUuid.forEach(name -> items.add(new PlayerInfo(name)));
        uuidToName.forEach((uuid, name) -> items.add(new PlayerInfo(name, uuid)));
        return items;
    }

    private void read() throws IOException {
        uuidToName.clear();
        nameToUuid.clear();
        namesWithoutUuid.clear();

        try (Reader reader = Files.newBufferedReader(jsonFilePath)) {
            PlayerItem[] data = GSON.fromJson(reader, PlayerItem[].class);
            if (data == null) {
                return;
            }
            for (PlayerItem item : data) {
                if (item == null || item.name() == null || item.name().isBlank()) {
                    continue;
                }
                String name = normalizeName(item.name());
                UUID uuid = parseUuid(item.uuid());
                if (uuid == null) {
                    if (!nameToUuid.containsKey(name)) {
                        namesWithoutUuid.add(name);
                    }
                } else if (!uuidToName.containsKey(uuid) && !nameToUuid.containsKey(name)) {
                    namesWithoutUuid.remove(name);
                    uuidToName.put(uuid, name);
                    nameToUuid.put(name, uuid);
                }
            }
        }
    }

    private void write() throws IOException {
        List<PlayerItem> items = new ArrayList<>(size());
        uuidToName.forEach((uuid, name) -> items.add(new PlayerItem(name, uuid.toString())));
        namesWithoutUuid.forEach(name -> items.add(new PlayerItem(name, "")));
        try (JsonWriter writer = GSON.newJsonWriter(Files.newBufferedWriter(jsonFilePath))) {
            writer.setIndent("  ");
            GSON.toJson(items.toArray(new PlayerItem[0]), PlayerItem[].class, writer);
        }
    }

    private String normalizeName(String name) {
        Objects.requireNonNull(name, "name");
        return playerNameCaseSensitive ? name : name.toLowerCase(java.util.Locale.ROOT);
    }

    private UUID parseUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private UUID findByName(String name) {
        UUID uuid = nameToUuid.get(name);
        if (uuid != null) {
            return uuid;
        }
        return namesWithoutUuid.contains(name) ? ZERO_UUID : null;
    }

    private String findByUuid(UUID uuid) {
        return uuidToName.get(uuid);
    }

    private void restoreAdd(String name, UUID uuid, boolean hadNameWithoutUuid) {
        if (uuid == null) {
            namesWithoutUuid.remove(name);
        } else {
            uuidToName.remove(uuid);
            nameToUuid.remove(name);
            if (hadNameWithoutUuid) {
                namesWithoutUuid.add(name);
            }
        }
    }

    private record PlayerItem(String name, String uuid) {
    }
}
