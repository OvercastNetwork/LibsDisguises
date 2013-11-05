package me.libraryaddict.disguise;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.minecraft.v1_6_R3.org.bouncycastle.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class BaseDisguiseCommand implements CommandExecutor {
    protected ArrayList<String> getAllowedDisguises(CommandSender sender, String permissionNode) {
        ArrayList<String> names = new ArrayList<String>();
        for (DisguiseType type : DisguiseType.values()) {
            String name = type.name().toLowerCase();
            if (sender.hasPermission("libsdisguises." + permissionNode + ".*")
                    || sender.hasPermission("libsdisguises." + permissionNode + "." + name))
                names.add(name);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    protected boolean isNumeric(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    protected boolean isDouble(String string) {
        try {
            Float.parseFloat(string);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    protected abstract void sendCommandUsage(CommandSender sender);

    /**
     * Returns the disguise if it all parsed correctly. Returns a exception with a complete message if it didn't. The
     * commandsender is purely used for checking permissions. Would defeat the purpose otherwise. To reach this point, the
     * disguise has been feed a proper disguisetype.
     */
    protected Disguise parseDisguise(CommandSender sender, String[] args) throws Exception {
        String permissionNode = getClass().getSimpleName().replace("Command", "").toLowerCase();
        ArrayList<String> allowedDisguises = getAllowedDisguises(sender, permissionNode);
        if (allowedDisguises.isEmpty()) {
            throw new Exception(ChatColor.RED + "You are forbidden to use this command.");
        }
        if (args.length == 0) {
            sendCommandUsage(sender);
            throw new Exception();
        }
        DisguiseType disguiseType = null;
        for (DisguiseType type : DisguiseType.values()) {
            if (args[0].equalsIgnoreCase(type.name()) || type.name().replace("_", "").equalsIgnoreCase(args[0])) {
                disguiseType = type;
                break;
            }
        }
        if (disguiseType == null) {
            throw new Exception(ChatColor.RED + "Error! The disguise " + ChatColor.GREEN + args[0] + ChatColor.RED
                    + " doesn't exist!");
        }
        if (!allowedDisguises.contains(args[0].toLowerCase())) {
            throw new Exception(ChatColor.RED + "You are forbidden to use this disguise!");
        }
        Disguise disguise = null;
        // How many args to skip due to the disugise being constructed
        int toSkip = 1;
        // Time to start constructing the disguise.
        // We will need to check between all 3 kinds of disguises
        if (disguiseType.isPlayer()) {// If he is doing a player disguise
            if (args.length == 1) {
                // He needs to give the player name
                throw new Exception(ChatColor.RED + "Error! You need to give a player name!");
            } else {
                // Construct the player disguise
                disguise = new PlayerDisguise(ChatColor.translateAlternateColorCodes('&', args[1]));
                toSkip++;
            }
        } else {
            if (disguiseType.isMob()) { // Its a mob, use the mob constructor
                boolean adult = true;
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                        adult = "false".equalsIgnoreCase(args[1]);
                        toSkip++;
                    }
                }
                disguise = new MobDisguise(disguiseType, adult);
            } else if (disguiseType.isMisc()) {
                // Its a misc, we are going to use the MiscDisguise constructor.
                int miscId = -1;
                int miscData = -1;
                if (args.length > 1) {
                    // They have defined more arguements!
                    // If the first arg is a number
                    if (isNumeric(args[1])) {
                        miscId = Integer.parseInt(args[1]);
                        toSkip++;
                        // If they also defined a data value
                        if (args.length > 2) {
                            if (isNumeric(args[2])) {
                                miscData = Integer.parseInt(args[2]);
                                toSkip++;
                            }
                        }
                    }
                }
                // Construct the disguise
                disguise = new MiscDisguise(disguiseType, true, miscId, miscData);
            }
        }
        // Copy strings to their new range
        String[] newArgs = new String[args.length - toSkip];
        for (int i = toSkip; i < args.length; i++) {
            newArgs[i - toSkip] = args[i];
        }
        args = newArgs;
        // Don't throw a error about uneven methods names and values so we can throw the error about what is unknown later.
        for (int i = 0; i < args.length; i += 2) {
            String methodName = args[i];
            if (i + 1 >= args.length) {
                throw new Exception(ChatColor.RED + "No value was given for " + methodName);
            }
            String valueString = args[i + 1];
            Method methodToUse = null;
            Object value = null;
            for (Method method : disguise.getWatcher().getClass().getMethods()) {
                if (!method.getName().startsWith("get") && method.getName().equalsIgnoreCase(methodName)) {
                    methodToUse = method;
                    methodName = method.getName();
                    Class<?>[] types = method.getParameterTypes();
                    if (types.length == 1) {
                        Class param = types[0];
                        if (float.class == param || double.class == param || int.class == param) {
                            if (isDouble(valueString)) {
                                float obj = Float.parseFloat(valueString);
                                if (param == float.class) {
                                    value = (float) obj;
                                } else if (param == int.class) {
                                    value = (int) obj;
                                } else if (param == double.class) {
                                    value = (double) obj;
                                }
                            } else {
                                throw new Exception(ChatColor.RED + "Expected a number, received " + valueString
                                        + " instead for " + methodName);
                            }
                        } else if (boolean.class == param) {
                            if (!("true".equalsIgnoreCase(valueString) || "false".equalsIgnoreCase(valueString)))
                                throw new Exception(ChatColor.RED + "Expected true/false, received " + valueString
                                        + " instead for " + methodName);
                            value = (boolean) "true".equalsIgnoreCase(valueString);
                        } else if (param == String.class) {
                            value = ChatColor.translateAlternateColorCodes('&', valueString);
                        }
                    }
                    break;
                }
            }
            if (methodToUse == null) {
                throw new Exception(ChatColor.RED + "Cannot find option " + methodName);
            }
            methodToUse.invoke(disguise.getWatcher(), value);
        }
        // Alright. We've constructed our disguise.
        return disguise;
    }
}