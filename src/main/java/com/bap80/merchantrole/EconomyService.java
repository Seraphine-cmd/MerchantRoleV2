package com.bap80.merchantrole;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Economy bridge. QuickShop-Hikari is the primary provider; Vault is used as a
 * fallback so the rank costs still work on servers with a classic Vault setup.
 * Reflection keeps the plugin compatible with multiple QuickShop releases.
 */
public final class EconomyService {
    private final MerchantRolePlugin plugin;

    private Object quickShopProvider;
    private Class<?> quickShopProviderClass;
    private Method quickShopCreateUser;
    private Method quickShopBalance;
    private Method quickShopWithdraw;
    private Method quickShopDeposit;
    private Method quickShopFormat;
    private Method quickShopValid;

    private Object vaultEconomy;
    private Class<?> vaultClass;
    private Method vaultBalance;
    private Method vaultWithdraw;
    private Method vaultDeposit;

    private String providerName = "none";

    public EconomyService(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        quickShopProvider = null;
        quickShopProviderClass = null;
        quickShopCreateUser = null;
        quickShopBalance = null;
        quickShopWithdraw = null;
        quickShopDeposit = null;
        quickShopFormat = null;
        quickShopValid = null;

        vaultEconomy = null;
        vaultClass = null;
        vaultBalance = null;
        vaultWithdraw = null;
        vaultDeposit = null;
        providerName = "none";

        if (setupQuickShop()) {
            return;
        }
        setupVault();
    }

    private boolean setupQuickShop() {
        try {
            Class<?> apiClass = Class.forName("com.ghostchu.quickshop.api.QuickShopAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object manager = apiClass.getMethod("getEconomyManager").invoke(api);

            Class<?> managerClass = Class.forName("com.ghostchu.quickshop.api.economy.EconomyManager");
            Object provider = managerClass.getMethod("provider").invoke(manager);
            if (provider == null) {
                return false;
            }

            quickShopProviderClass = Class.forName("com.ghostchu.quickshop.api.economy.EconomyProvider");
            Method valid = quickShopProviderClass.getMethod("valid");
            if (!Boolean.TRUE.equals(valid.invoke(provider))) {
                return false;
            }

            Class<?> userClass = Class.forName("com.ghostchu.quickshop.api.obj.QUser");
            Class<?> userImpl = Class.forName("com.ghostchu.quickshop.obj.QUserImpl");

            quickShopCreateUser = userImpl.getMethod("createFullFilled", Player.class);
            quickShopBalance = quickShopProviderClass.getMethod("balance", userClass, String.class, String.class);
            quickShopWithdraw = quickShopProviderClass.getMethod("withdraw", userClass, String.class, String.class, BigDecimal.class);
            quickShopDeposit = quickShopProviderClass.getMethod("deposit", userClass, String.class, String.class, BigDecimal.class);
            quickShopFormat = quickShopProviderClass.getMethod("format", BigDecimal.class, String.class, String.class);
            quickShopValid = valid;

            Object name = quickShopProviderClass.getMethod("providerName").invoke(provider);
            providerName = "QuickShop-Hikari (" + name + ")";
            quickShopProvider = provider;
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not hook QuickShop-Hikari economy: " + exception.getMessage());
            return false;
        }
    }

    private void setupVault() {
        try {
            vaultClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(vaultClass);
            if (registration == null || registration.getProvider() == null) {
                return;
            }
            vaultEconomy = registration.getProvider();
            vaultBalance = vaultClass.getMethod("balance", OfflinePlayer.class);
            vaultWithdraw = vaultClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            vaultDeposit = vaultClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            providerName = "Vault";
        } catch (ClassNotFoundException ignored) {
            // Vault is not installed.
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not hook Vault economy: " + exception.getMessage());
        }
    }

    public boolean available() {
        return quickShopProvider != null || vaultEconomy != null;
    }

    public String providerName() {
        return providerName;
    }

    public double balance(Player player) {
        if (quickShopProvider != null) {
            try {
                Object user = quickShopCreateUser.invoke(null, player);
                Object balance = quickShopBalance.invoke(quickShopProvider, user,
                        player.getWorld().getName(), null);
                return ((BigDecimal) balance).doubleValue();
            } catch (ReflectiveOperationException | RuntimeException exception) {
                plugin.getLogger().warning("QuickShop balance lookup failed: " + exception.getMessage());
                return 0;
            }
        }
        if (vaultEconomy != null) {
            try {
                return ((Number) vaultBalance.invoke(vaultEconomy, (OfflinePlayer) player)).doubleValue();
            } catch (ReflectiveOperationException | RuntimeException exception) {
                plugin.getLogger().warning("Vault balance lookup failed: " + exception.getMessage());
            }
        }
        return 0;
    }

    public boolean has(Player player, double amount) {
        return amount <= 0 || balance(player) >= amount;
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (quickShopProvider != null) {
            try {
                Object user = quickShopCreateUser.invoke(null, player);
                Object result = quickShopWithdraw.invoke(quickShopProvider, user,
                        player.getWorld().getName(), null, money(amount));
                return Boolean.TRUE.equals(result);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                plugin.getLogger().warning("QuickShop withdrawal failed: " + exception.getMessage());
                return false;
            }
        }
        if (vaultEconomy != null) {
            try {
                Object response = vaultWithdraw.invoke(vaultEconomy, (OfflinePlayer) player, amount);
                return response != null
                        && Boolean.TRUE.equals(response.getClass().getMethod("transactionSuccess").invoke(response));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                plugin.getLogger().warning("Vault withdrawal failed: " + exception.getMessage());
            }
        }
        return false;
    }

    public boolean deposit(Player player, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (quickShopProvider != null) {
            try {
                Object user = quickShopCreateUser.invoke(null, player);
                Object result = quickShopDeposit.invoke(quickShopProvider, user,
                        player.getWorld().getName(), null, money(amount));
                return Boolean.TRUE.equals(result);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                plugin.getLogger().warning("QuickShop deposit failed: " + exception.getMessage());
                return false;
            }
        }
        if (vaultEconomy != null) {
            try {
                Object response = vaultDeposit.invoke(vaultEconomy, (OfflinePlayer) player, amount);
                return response != null
                        && Boolean.TRUE.equals(response.getClass().getMethod("transactionSuccess").invoke(response));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                plugin.getLogger().warning("Vault deposit failed: " + exception.getMessage());
            }
        }
        return false;
    }

    public String format(double amount) {
        if (quickShopProvider != null) {
            try {
                Object formatted = quickShopFormat.invoke(quickShopProvider, money(amount), "world", null);
                if (formatted != null) {
                    return formatted.toString();
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fall through to the generic format.
            }
        }
        if (vaultEconomy != null) {
            try {
                Method format = vaultClass.getMethod("format", double.class);
                Object formatted = format.invoke(vaultEconomy, amount);
                if (formatted != null) {
                    return formatted.toString();
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fall through to the generic format.
            }
        }
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    private static BigDecimal money(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
