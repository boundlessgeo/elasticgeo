/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package mil.nga.giat.elasticsearch;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

import mil.nga.giat.data.elasticsearch.ElasticAttribute;
import mil.nga.giat.data.elasticsearch.ElasticDataStore;
import mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.resource.ResourceConfigurationPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

/**
 * Resource configuration panel to show a link to open Elasticsearch attribute 
 * modal dialog <br> If the Elasticsearch attribute are not configured for 
 * current layer, the modal dialog will be open at first resource configuration 
 * window opening <br> After modal dialog is closed the resource page is 
 * reloaded and feature configuration table updated
 * 
 */
public class ElasticConfigurationPanel extends ResourceConfigurationPanel {

    private static final long serialVersionUID = 3382530429105288433L;

    /**
     * Adds Elasticsearch configuration panel link, configure modal dialog and 
     * implements modal callback.
     * 
     * @see {@link ElasticConfigurationPage#done}
     */

    public ElasticConfigurationPanel(final String panelId, final IModel model) {
        super(panelId, model);
        final FeatureTypeInfo fti = (FeatureTypeInfo) model.getObject();

        final ModalWindow modal = new ModalWindow("modal");
        modal.setInitialWidth(800);
        modal.setTitle(new ParamResourceModel("modalTitle", ElasticConfigurationPanel.this));

        if (fti.getMetadata().get(ElasticLayerConfiguration.KEY) == null) {
            modal.add(new OpenWindowOnLoadBehavior());
        }

        modal.setContent(new ElasticConfigurationPage(panelId, model) {
            @Override
            void done(AjaxRequestTarget target, ElasticLayerConfiguration layerConfig) {
                if (layerConfig != null) {
                    ResourceInfo ri = ElasticConfigurationPanel.this.getResourceInfo();
                    ri.getMetadata().put(ElasticLayerConfiguration.KEY, layerConfig);
                    
                    MarkupContainer parent = ElasticConfigurationPanel.this.getParent();
                    while (!(parent == null || parent instanceof ResourceConfigurationPage)) {
                        parent = parent.getParent();
                    }
                    if (parent != null && parent instanceof ResourceConfigurationPage) {
                        ((ResourceConfigurationPage)parent).updateResource(ri, target);
                    }
                }
                modal.close(target);
            }
        });
        add(modal);

        AjaxLink findLink = new AjaxLink("edit") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                modal.show(target);
            }
        };
        final Fragment attributePanel = new Fragment("esPanel", "esPanelFragment", this);
        attributePanel.setOutputMarkupId(true);
        add(attributePanel);
        attributePanel.add(findLink);
    }

    /*
     * Open modal dialog on window load
     */
    private class OpenWindowOnLoadBehavior extends AbstractDefaultAjaxBehavior {
        @Override
        protected void respond(AjaxRequestTarget target) {
            ModalWindow window = (ModalWindow) getComponent();
            window.show(target);
        }

        @Override
        public void renderHead(Component component, IHeaderResponse response) {
            response.render(OnLoadHeaderItem.forScript(getCallbackScript().toString()));
        }
    }
}
