/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 *
 * Chandra Dhulipala - June 2018 - Modified to display user messages
 */
// simon.moffatt@forgerock.com - retrieves profile attrbute and checks for specified value

package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.i18n.PreferredLocales;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;

@Node.Metadata(outcomeProvider = ProfileAttributeDecisionNode.OutcomeProvider.class,
        configClass = ProfileAttributeDecisionNode.Config.class)
public class ProfileAttributeDecisionNode implements Node {

    private final static String DEBUG_FILE = "ProfileAttributeDecisionNode";
    private static final String INPUT = "mail";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private final CoreWrapper coreWrapper;

    /**
     * Configuration for the node.
     */
    public interface Config {

        //Property to search for
        @Attribute(order = 100)
        default String profileAttribute() {return "";
        }

        @Attribute(order = 200)
        default String profileAttributeValue() {
            return "";
        }


    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public ProfileAttributeDecisionNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
   
        debug.message("[" + DEBUG_FILE + "]: " + "Starting in ProfileAttributeDecisionNode");

        // Pull out the user object. Query on different attributes
        Set<String> userAttributes = new HashSet<>();
        userAttributes.add("uid");
        userAttributes.add("mail");
        AMIdentity userIdentity = IdUtils.getIdentity(context.sharedState.get(INPUT).asString(), context.sharedState.get(REALM).asString(), userAttributes);

        //Pull out the specified attribute
        debug.message("[" + DEBUG_FILE + "]: Looking for profile attribute " + config.profileAttribute());

        try {

                Set<String> idAttrs = userIdentity.getAttribute(config.profileAttribute());

                if (idAttrs == null || idAttrs.isEmpty() ) {

                    debug.error("[" + DEBUG_FILE + "]: " + "Unable to find attribute " + config.profileAttribute() + "on user profile");
                    return goTo("Empty").build();

                } else {

                    String attr = idAttrs.iterator().next();
                    debug.message("[" + DEBUG_FILE + "]: " + "Found attribute value for: " + config.profileAttribute() + " as " + attr);
                    

                    //Check the attribute value found matches submitted
                    if(attr.equals(config.profileAttributeValue())) {

                        debug.message("[" + DEBUG_FILE + "]: " + "Found attribute value and matches submitted value");
                        return goTo("Match").build();

                    } else {

                        debug.message("[" + DEBUG_FILE + "]: " + "Found attribute but value doesn't match submitted value");

                        if(context.hasCallbacks()) {
                            debug.message("[" + DEBUG_FILE + "]: " + "hasCallbacks: " );
                                return goTo("noMatch").build();

                        } else {
                            debug.message("[" + DEBUG_FILE + "]: " + " no Callbacks: " );
                            List<Callback> callbacks = new ArrayList<Callback>(1);
                            String errMsg = "Your account might be locked or made inactive. Please contact your primary DSCRO.";
                            TextOutputCallback tocb = new TextOutputCallback(TextOutputCallback.INFORMATION, errMsg);
                            callbacks.add(tocb);
                            return send(ImmutableList.copyOf(callbacks)).build();
                        }
                    }
                }
            } catch (IdRepoException e) {

                debug.error("[" + DEBUG_FILE + "]: " + " Error storing profile atttibute '{}' ", e);

            } catch (SSOException e) {

                debug.error("[" + DEBUG_FILE + "]: " + "Node exception", e);
            }
        //No match found outcome
        return goTo("noMatch").build();

    }

    private Action.ActionBuilder goTo(String outcome) {
        return Action.goTo(outcome);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = ProfileAttributeDecisionNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome( "Empty", bundle.getString("Empty")),
                    new Outcome("Match", bundle.getString("Match")),
                    new Outcome("noMatch", bundle.getString("noMatch")));
        }
    }
}
