/**
 *
 */
package com.emc.estore.core.cronjob;

import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.exceptions.ModelRemovalException;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;


/**
 * @author smasthan
 *
 */
public class EstoreDuplicateIdentifiersMediaCleanupJob extends AbstractJobPerformable<CronJobModel>
{

	/**
	 * @return the flexibleSearchService
	 */
	public FlexibleSearchService getFlexibleSearchService()
	{
		return flexibleSearchService;
	}

	/**
	 * @param flexibleSearchService
	 *           the flexibleSearchService to set
	 */
	@Override
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

	private FlexibleSearchService flexibleSearchService;

	private static final Logger LOG = Logger.getLogger(EstoreDuplicateIdentifiersMediaCleanupJob.class.getName());

	@Override
	public PerformResult perform(final CronJobModel arg0)
	{
		boolean caughtException = false;


		final List<MediaModel> allMedia = getAllMedia();

		final List<MediaModel> allDuplicateMedia = new ArrayList<MediaModel>();
		final List<MediaModel> duplicateMedia = new ArrayList<MediaModel>();
		final Set<String> mediaData = new HashSet<String>();
		allDuplicateMedia.addAll(allMedia);



		if (CollectionUtils.isNotEmpty(allDuplicateMedia))
		{

			for (final MediaModel lastModified : allDuplicateMedia)
			{
				try
				{
					//	mediaData.add(lastModified.getCode());
					if (mediaData.add(lastModified.getCode()))
					{
						mediaData.add(lastModified.getCode());
					}
					else
					{
						duplicateMedia.add(lastModified);
					}

				}
				catch (final Exception e1)
				{
					LOG.error(e1.getMessage());
					caughtException = true;
				}

			}
			try
			{
				LOG.info("Number of Duplicate Identifiers media's found =" + duplicateMedia.size());

				modelService.removeAll(duplicateMedia);
				LOG.info("Duplicate Identifier Medias were successfully removed at " + new Date());
			}
			catch (final ModelRemovalException e)
			{
				LOG.error(e.getMessage());
				caughtException = true;
			}
		}
		else
		{
			LOG.info("No Duplicate Identifiers Medias found");
		}
		return new PerformResult(caughtException ? CronJobResult.FAILURE : CronJobResult.SUCCESS, CronJobStatus.FINISHED);
	}

	private List<MediaModel> getAllMedia()
	{

		final StringBuilder query = new StringBuilder(
				"SELECT {m.pk} FROM {Media! as m} where EXISTS ({{ SELECT MIN({p.pk}) as mediacount FROM {Media! as p} WHERE {p.catalogVersion} IS NOT NULL AND {p.catalogversion} IN ( {{ SELECT {v.pk} FROM {CatalogVersion as v } WHERE {v.catalog} IN ({{ SELECT {c.pk} FROM {Catalog as c} WHERE {c.id} IN ('eStoreProductCatalog','eStoreContentCatalog') }}) }}) group by {p.code} having count(*) > 1 }}) ORDER BY MODIFIEDTS DESC ");
		//	"SELECT {m.code} FROM {Media! as m} where EXISTS ({{ SELECT MIN({p.pk}) as mediacount FROM {Media! as p} WHERE {p.catalogVersion} IS NOT NULL AND {p.catalogversion} IN ( {{ SELECT {v.pk} FROM {CatalogVersion as v } WHERE {v.catalog} IN ({{ SELECT {c.pk} FROM {Catalog as c} WHERE {c.id} IN ('eStoreProductCatalog','eStoreContentCatalog') }}) }}) group by {p.code} having count(*) > 1 }}) ORDER BY MODIFIEDTS DESC ");

		final FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(query.toString());

		final SearchResult<MediaModel> searchResult = getFlexibleSearchService().search(flexibleSearchQuery);


		return searchResult.getResult();
	}
}
