package net.tridentsdk.server.ui.tablist;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import lombok.Getter;
import net.tridentsdk.chat.ChatComponent;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.server.packet.play.PlayOutPlayerListHeaderAndFooter;
import net.tridentsdk.server.packet.play.PlayOutTabListItem;
import net.tridentsdk.server.player.TridentPlayer;
import net.tridentsdk.ui.tablist.TabList;

import java.util.Collection;
import java.util.Collections;

/**
 * The tab list implementation.
 */
public abstract class TridentTabList implements TabList {
    /**
     * The players which are displayed this tab list
     */
    private final Collection<Player> users;
    /**
     * The tab list header
     */
    @Getter
    private volatile ChatComponent header;
    /**
     * Tab list footer
     */
    @Getter
    private volatile ChatComponent footer;
    /**
     * Elements of this tab list
     */
    protected final Collection<TabListElement> elements;

    /**
     * Creates and initailizes a new tab list/
     * superconstructor
     */
    public TridentTabList() {
        this.users = Sets.newConcurrentHashSet();
        this.elements = Queues.newConcurrentLinkedQueue();
    }

    @Override
    public void setHeader(ChatComponent value) {
        this.header = value;
        this.updateHeaderFooter();
    }

    @Override
    public void setFooter(ChatComponent value) {
        this.footer = value;
        this.updateHeaderFooter();
    }

    @Override
    public Collection<Player> getUserList() {
        return Collections.unmodifiableCollection(this.users);
    }

    @Override
    public void addUser(Player player) {
        this.users.add(player);
    }

    @Override
    public void removeUser(Player player) {
        this.users.remove(player);
    }

    /**
     * Sends the tab list to the given player.
     *
     * @param player the player to send the tab list
     */
    public void sendToPlayer(TridentPlayer player) {
        PlayOutTabListItem.PlayOutTabListItemAddPlayer itemPacket = PlayOutTabListItem.addPlayerPacket();
        this.elements.forEach(element -> itemPacket.addPlayer(element.getUuid(), element.getName(), element.getGameMode(), element.getPing(), element.getDisplayName()));
        player.net().sendPacket(itemPacket);

        PlayOutPlayerListHeaderAndFooter headerAndFooterPacket = new PlayOutPlayerListHeaderAndFooter(this.header, this.footer);
        player.net().sendPacket(headerAndFooterPacket);
    }

    /**
     * Update operation if the header or footer fields of
     * the tab list are updated.
     */
    private void updateHeaderFooter() {
        PlayOutPlayerListHeaderAndFooter packet = new PlayOutPlayerListHeaderAndFooter(this.header, this.footer);
        this.getUserList().forEach(player -> ((TridentPlayer) player).net().sendPacket(packet));
    }
}