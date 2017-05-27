package com.dotmarketing.db;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.bulk.BulkRequestBuilder;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableList;

public abstract class ReindexRunnable extends DotRunnable {

	public enum Action{ADDING, REMOVING, BE_SMART};

	
	private final Action action;
	private final List<String> contentToIndex;
	private final BulkRequestBuilder bulk;
	private boolean reindexOnly;

	public List<String> getReindexIds() {
		return contentToIndex;
	}

	public ReindexRunnable(List<Contentlet> reindexCons, Action action, BulkRequestBuilder bulk, boolean reindexOnly) {
		super();

		this.contentToIndex =reindexCons.stream()
            .map(Contentlet::getIdentifier)
            .collect(Collectors.toList());

		this.action = action;
		this.bulk = bulk;
		this.reindexOnly = reindexOnly;
	}
	
    public ReindexRunnable(List<String> reindexCons, Action action, BulkRequestBuilder bulk, boolean reindexOnly, boolean nothing) {
      super();
      this.contentToIndex =reindexCons;

      this.action = action;
      this.bulk = bulk;
      this.reindexOnly = reindexOnly;
  }
	public ReindexRunnable(Contentlet reindexId, Action action, BulkRequestBuilder bulk) {
		super();

		contentToIndex = ImmutableList.of(reindexId.getIdentifier());
		this.action = action;
		this.bulk = bulk;
	}
    public ReindexRunnable(String reindexId, Action action) {
      super();

      contentToIndex = ImmutableList.of(reindexId);
      this.action = action;
      this.bulk = null;
  }
	public Action getAction() {
		return action;
	}

    public void run() {

        try {
        	if(action.equals(Action.ADDING)){
        		APILocator.getContentletIndexAPI().indexContentIds(contentToIndex, bulk, reindexOnly);
        	}
        	else{
        		throw new DotStateException("REMOVE ACTION NEEDS TO OVERRIDE THE run() method");
        	}
        } catch (Exception e) {
			throw new RuntimeException(e);
        }
    }
    
}
