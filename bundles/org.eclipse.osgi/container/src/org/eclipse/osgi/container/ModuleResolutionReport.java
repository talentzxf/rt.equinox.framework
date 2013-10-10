/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import java.util.*;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.*;
import org.osgi.service.resolver.ResolutionException;

/**
 * @since 3.10
 */
public class ModuleResolutionReport implements ResolutionReport {

	public static class Builder {
		private final Map<Resource, List<Entry>> resourceToEntries = new HashMap<Resource, List<Entry>>();

		public void addEntry(Resource resource, Entry.Type type, Object data) {
			List<Entry> entries = resourceToEntries.get(resource);
			if (entries == null) {
				entries = new ArrayList<Entry>();
				resourceToEntries.put(resource, entries);
			}
			entries.add(new EntryImpl(type, data));
		}

		public ModuleResolutionReport build(Map<Resource, List<Wire>> resolutionResult, ResolutionException cause) {
			return new ModuleResolutionReport(resolutionResult, resourceToEntries, cause);
		}
	}

	private static class EntryImpl implements Entry {
		private final Object data;
		private final Type type;

		EntryImpl(Type type, Object data) {
			this.type = type;
			this.data = data;
		}

		@Override
		public Object getData() {
			return data;
		}

		@Override
		public Type getType() {
			return type;
		}
	}

	private final Map<Resource, List<Entry>> entries;
	private final ResolutionException resolutionException;
	private final Map<Resource, List<Wire>> resolutionResult;

	ModuleResolutionReport(Map<Resource, List<Wire>> resolutionResult, Map<Resource, List<Entry>> entries, ResolutionException cause) {
		this.entries = entries == null ? Collections.<Resource, List<Entry>> emptyMap() : Collections.unmodifiableMap(new HashMap<Resource, List<ResolutionReport.Entry>>(entries));
		this.resolutionResult = resolutionResult == null ? Collections.<Resource, List<Wire>> emptyMap() : Collections.unmodifiableMap(resolutionResult);
		this.resolutionException = cause;
	}

	@Override
	public Map<Resource, List<Entry>> getEntries() {
		return entries;
	}

	@Override
	public ResolutionException getResoltuionException() {
		return resolutionException;
	}

	Map<Resource, List<Wire>> getResolutionResult() {
		return resolutionResult;
	}

	private static String getResolutionReport0(String prepend, ModuleRevision revision, Map<Resource, List<ResolutionReport.Entry>> reportEntries, Set<BundleRevision> visited) {
		if (prepend == null) {
			prepend = ""; //$NON-NLS-1$
		}
		if (visited == null) {
			visited = new HashSet<BundleRevision>();
		}
		if (visited.contains(revision)) {
			return ""; //$NON-NLS-1$
		}
		visited.add(revision);
		StringBuilder result = new StringBuilder();
		String id = revision.getRevisions().getModule().getId().toString();
		result.append(prepend).append(revision.getSymbolicName()).append(" [").append(id).append("]").append('\n'); //$NON-NLS-1$ //$NON-NLS-2$

		List<ResolutionReport.Entry> revisionEntries = reportEntries.get(revision);
		if (revisionEntries == null) {
			result.append(prepend).append("  ").append(Msg.ModuleResolutionReport_NoReport); //$NON-NLS-1$
		} else {
			for (ResolutionReport.Entry entry : revisionEntries) {
				printResolutionEntry(result, prepend + "  ", entry, reportEntries, visited); //$NON-NLS-1$
			}
		}
		return result.toString();
	}

	private static void printResolutionEntry(StringBuilder result, String prepend, ResolutionReport.Entry entry, Map<Resource, List<ResolutionReport.Entry>> reportEntries, Set<BundleRevision> visited) {
		switch (entry.getType()) {
			case MISSING_CAPABILITY :
				result.append(prepend).append(Msg.ModuleResolutionReport_UnresolvedReq).append(entry.getData()).append('\n');
				break;
			case SINGLETON_SELECTION :
				result.append(prepend).append(Msg.ModuleResolutionReport_AnotherSingleton).append(entry.getData()).append('\n');
				break;
			case UNRESOLVED_PROVIDER :
				@SuppressWarnings("unchecked")
				Map<Requirement, Set<Capability>> unresolvedProviders = (Map<Requirement, Set<Capability>>) entry.getData();
				for (Map.Entry<Requirement, Set<Capability>> unresolvedRequirement : unresolvedProviders.entrySet()) {
					// for now only printing the first possible unresolved candidates
					Set<Capability> unresolvedCapabilities = unresolvedRequirement.getValue();
					if (!unresolvedCapabilities.isEmpty()) {
						Capability unresolvedCapability = unresolvedCapabilities.iterator().next();
						// make sure this is not a case of importing and exporting the same package
						if (!unresolvedRequirement.getKey().getResource().equals(unresolvedCapability.getResource())) {
							result.append(prepend).append(Msg.ModuleResolutionReport_UnresolvedReq).append(unresolvedRequirement.getKey()).append('\n');
							result.append(prepend).append("  -> ").append(unresolvedCapability).append('\n'); //$NON-NLS-1$
							result.append(getResolutionReport0(prepend + "     ", (ModuleRevision) unresolvedCapability.getResource(), reportEntries, visited)); //$NON-NLS-1$
						}
					}
				}
				break;
			case FILTERED_BY_RESOLVER_HOOK :
				result.append(Msg.ModuleResolutionReport_FilteredByHook).append('\n');
				break;
			case USES_CONSTRAINT_VIOLATION :
				result.append(prepend).append(Msg.ModuleResolutionReport_UsesConstraintError).append('\n');
				result.append("  ").append(entry.getData()); //$NON-NLS-1$
				break;
			default :
				result.append(Msg.ModuleResolutionReport_Unknown).append("type=").append(entry.getType()).append(" data=").append(entry.getData()).append('\n'); //$NON-NLS-1$ //$NON-NLS-2$
				break;
		}
	}

	@Override
	public String getResolutionReportMessage(Resource resource) {
		return getResolutionReport0(null, (ModuleRevision) resource, getEntries(), null);
	}
}