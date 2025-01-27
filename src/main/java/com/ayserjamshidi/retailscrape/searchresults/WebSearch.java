package com.ayserjamshidi.retailscrape.searchresults;

import com.ayserjamshidi.retailscrape.RetailConfig;
import com.ayserjamshidi.retailscrape.RetailScrape;
import com.ayserjamshidi.retailscrape.ThreadList.AmazonTrackingListUSA;
import com.ayserjamshidi.retailscrape.addons.discord.DiscordChannel;
import com.ayserjamshidi.retailscrape.searchresults.itemtemplate.AmazonSearchItem;
import com.ayserjamshidi.retailscrape.searchresults.itemtemplate.BestBuySearchItem;
import com.ayserjamshidi.retailscrape.searchresults.itemtemplate.NeweggSearchItem;
import com.ayserjamshidi.retailscrape.searchresults.itemtemplate.TemplateSearchItem;
import com.ayserjamshidi.retailscrape.threads.DiscordAnnouncer;
import org.openqa.selenium.*;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/// TODO: If we've announced, and add to cart is found, don't announce until add to cart hasn't been found for at least 15 refreshes!
public class WebSearch extends Thread {
    // Pre-set variables variables
    final int REFRESH_ATTEMPTS = 15;
    final int ERROR_WAIT_MINUTES = 4;
    final int GENERAL_WAIT_MINUTES = 60 * 6; // 35;
    final int SAME_ITEM_WAIT_MINUTES = 60 * 6;

    // Set on run-time
    final int MIN_WAIT_SECONDS;
    final int MAX_WAIT_SECONDS;

    // Dynamics
    int amountOfCombosAnnounced = 0;
    long comboTimeLimit = 0;
    HashMap<String, Long> announcedItems = new HashMap<>();
    DiscordChannel discordChannel;
    By itemSearchTerm;
    private long lastComplaint = 0;
    String[] pageUrls;
    String lastAnnouncedItem = "";

    // Prevents spam announcing if item is always cart-able
    HashMap<String, Long> addToCartFound;

    // Driver setup
    WebDriver driver;
    Proxy proxy;

    // Special variables for Amazon
    AmazonTrackingListUSA amazonItemDetails;

    // Prevents us from switching proxies too quickly
    long lastGoodLoad = System.currentTimeMillis(); // Current time because if set to 0, would be pretty weird

    public WebSearch(final String threadName, final DiscordChannel discordChannel, final int MIN_WAIT_SECONDS,
                     final int MAX_WAIT_SECONDS, final String[] pageUrls, final By itemSearchTerm) {
        this.setName(threadName);
        sendMessage("Initializing");

        this.pageUrls = pageUrls;
        this.discordChannel = discordChannel;
        this.MIN_WAIT_SECONDS = MIN_WAIT_SECONDS;
        this.MAX_WAIT_SECONDS = MAX_WAIT_SECONDS;
        this.itemSearchTerm = itemSearchTerm;
    }

    @Override
    public void run() {
        List<TemplateSearchItem> normalItemList = new ArrayList<>();
        List<TemplateSearchItem> comboItemList = new ArrayList<>();

        while (!this.isInterrupted()) {
            for (final String currentUrl : pageUrls) {
                reloadPage(currentUrl);
                afterLoadTasks();
                sleep(5000, false);
                sendMessage(driver.getTitle() + " - Last good load: " + (System.currentTimeMillis() - lastGoodLoad) / 60000L + " minutes ago");

                try {
                    if (pageReloaded()) {
                        lastGoodLoad = System.currentTimeMillis();
                        for (final WebElement curPossibleItem : driver.findElements(itemSearchTerm)) {
                            if (!itemIsValid(curPossibleItem))
                                continue;

                            final TemplateSearchItem currentItem = itemCreator(curPossibleItem);

                            if (itemIsAvailable(currentItem)) {
                                if (RetailConfig.OPEN_IN_BROWSER_MODE) {
                                    Desktop.getDesktop().browse(new URI(currentUrl));
                                } else {
                                    if (currentItem.isCombo) {//itemIsCombo(currentItem)) {
                                        if (System.currentTimeMillis() - comboTimeLimit < minutes(15))
                                            continue;

//									if (comboItemList.size() >= 10)
//										continue;

                                        if (amountOfCombosAnnounced >= 2) {
                                            comboTimeLimit = System.currentTimeMillis();

                                            new Thread() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        Thread.sleep(minutes(30));
                                                        amountOfCombosAnnounced = 0;
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            };
                                            continue;
                                        }
                                        comboItemList.add(currentItem);
                                        amountOfCombosAnnounced++;
                                    } else {
//									if (normalItemList.size() >= 10)
//										continue;

                                        normalItemList.add(currentItem);
                                    }
                                }

                                sendMessage("[IN STOCK] " + currentItem.itemName);
                                lastAnnouncedItem = currentItem.itemName;
                                announcedItems.put(currentItem.itemName, System.currentTimeMillis());
                            }
                        }

                        // Wipe both lists so we don't spam announce
                        if (normalItemList.size() > 0) {
                            DiscordAnnouncer.queueAnnounce(discordChannel, normalItemList);
                            normalItemList.clear();
                        }
                        if (comboItemList.size() > 0) {
//							DiscordAnnouncer.queueAnnounce(discordChannel, comboItemList);
                            comboItemList.clear();
                        }

                    } else {//canComplain()) {
//						sendMessage("Complaining time");
                        badPageReload();
                        continue; // Skip at this point so we don't hit the sleep as we want to immediately try and fix the bad reload.
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                sleep(secondsRand(MIN_WAIT_SECONDS, MAX_WAIT_SECONDS), false);
            }
        }

        DiscordAnnouncer.queueError("[" + this.getName() + "] - Thread has crashed for some unknown reason. Restart the bot!");
    }

    protected void afterLoadTasks() {
    }

	/*protected boolean itemIsCombo(final TemplateSearchItem obj) {
		return false;
	}*/

    protected boolean itemIsValid(final WebElement element) throws Exception {
        return true;
    }

    protected boolean pageReloaded() {
        return true;
    }

    protected void badPageReload() throws Exception {
        throw new Exception("[" + this.getName() + "] - badPageReload is not overridden.");
    }

    protected String getFirstElementText(final WebElement element, final By searchType) {
        try {
            return element.findElement(searchType).getText();
        } catch (Exception ex) {
            return null;
        }
    }

    protected WebElement getFirstElement(final WebElement element, final By searchType) {
        try {
            return element.findElement(searchType);
        } catch (Exception ex) {
            return null;
        }
    }

    protected void setupProxy() {
        if (RetailScrape.proxyList.size() > 0) {
            if (RetailScrape.proxyIndex >= RetailScrape.proxyList.size()) // If OOB
                RetailScrape.proxyIndex = 0; // Start at the beginning again

            proxy.setHttpProxy(RetailScrape.proxyList.get(RetailScrape.proxyIndex));
            proxy.setSslProxy(RetailScrape.proxyList.get(RetailScrape.proxyIndex++));

            sendMessage("IP is now " + proxy.getHttpProxy());
        } else {
            sendMessage("Can't setup proxy, we're out!");
        }
    }

    private TemplateSearchItem itemCreator(final WebElement curPossibleItem) {
        switch (this.getClass().getSimpleName()) {
            case "NeweggSearch":
                return new NeweggSearchItem(curPossibleItem);
            case "BestBuySearch":
                return new BestBuySearchItem(curPossibleItem);
            case "AmazonSearch":
                return new AmazonSearchItem(curPossibleItem, amazonItemDetails);
            default:
                return null;
        }
    }

    private boolean itemIsAvailable(TemplateSearchItem givenItem) {
        if (givenItem == null)
            return false;

        if (!givenItem.isAvailable()) {
//			sendMessage("[OUT OF STOCK] " + givenItem.itemName);
            return false;
        }

        if (!canAnnounce(givenItem.itemName)) {
            sendMessage("[IN STOCK - SKIPPED] " + givenItem.itemName);
            return false;
        }

        return true;
    }

    protected void reloadPage(final String currentUrl) {
        for (int i = 0; i < REFRESH_ATTEMPTS; i++)
            try {
                driver.get(currentUrl);
                break;
            } catch (WebDriverException ex) {
                ex.printStackTrace();
                DiscordAnnouncer.queueError("[" + this.getName() + "] - Tried loading the assigned URL but caught an exception.");
            }
    }

    private boolean canAnnounce(final String itemName) {
        if (!announcedItems.containsKey(itemName))
            return true;

        if (lastAnnouncedItem != null && lastAnnouncedItem.equals(itemName))
            return System.currentTimeMillis() - announcedItems.get(itemName) > minutes(SAME_ITEM_WAIT_MINUTES);

        return System.currentTimeMillis() - announcedItems.get(itemName) > minutes(GENERAL_WAIT_MINUTES);
    }

    private boolean canComplain() {
        if (System.currentTimeMillis() - lastComplaint > minutes(ERROR_WAIT_MINUTES)) {
            lastComplaint = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    protected int minutes(final int i) {
        return i * 60000;
    }

    protected int secondsRand(final int minSeconds, final int maxSeconds) {
        return (new Random().nextInt(maxSeconds - minSeconds) + minSeconds) * 1000;
    }

    protected void sleep(final int ms, boolean announce) {
        try {
            if (announce)
                sendMessage("Sleeping for " + ms / 1000 + " seconds.");
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void sendMessage(String message) {
        System.out.println("[" + this.getName() + "] - " + message);
    }
}