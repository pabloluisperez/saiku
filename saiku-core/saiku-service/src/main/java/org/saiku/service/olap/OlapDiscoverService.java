/*
 * Copyright (C) 2011 Paul Stoellberger
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 *
 */
package org.saiku.service.olap;

import java.util.List;

import org.olap4j.OlapConnection;
import org.olap4j.metadata.Cube;
import org.saiku.olap.discover.OlapMetaExplorer;
import org.saiku.olap.dto.SaikuConnection;
import org.saiku.olap.dto.SaikuCube;
import org.saiku.olap.dto.SaikuDimension;
import org.saiku.olap.dto.SaikuHierarchy;
import org.saiku.olap.dto.SaikuLevel;
import org.saiku.olap.dto.SaikuMember;
import org.saiku.olap.util.exception.SaikuOlapException;
import org.saiku.service.datasource.DatasourceService;
import org.saiku.service.util.exception.SaikuServiceException;

public class OlapDiscoverService {
	
	private DatasourceService datasourceService;
	private OlapMetaExplorer metaExplorer;
	
	public void setDatasourceService(DatasourceService ds) {
		datasourceService = ds;
		metaExplorer = new OlapMetaExplorer(datasourceService.getOlapConnections());
	}
	
	public List<SaikuCube> getAllCubes() {
		return metaExplorer.getAllCubes();
	}

	public List<SaikuConnection> getAllConnections() throws SaikuServiceException {
		try {
			return metaExplorer.getAllConnections();
		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot retrieve all connections", e);
		}
	}
	
	public Cube getNativeCube(SaikuCube cube) throws SaikuServiceException {
		try {
			return metaExplorer.getNativeCube(cube);
		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot get native cube for cube ( " + cube + " )", e);
		}
	}

	public OlapConnection getNativeConnection(String name) throws SaikuServiceException {
		try {
			return metaExplorer.getNativeConnection(name);
		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot get native connection for cube ( " + name + " )", e);
		}
	}

	public List<SaikuDimension> getAllDimensions(SaikuCube cube) throws SaikuServiceException {
		try {
			return metaExplorer.getAllDimensions(cube);
		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot get all dimensions for cube ( " + cube + " )", e);
		}
	}
	
	public List<SaikuHierarchy> getAllHierarchies(SaikuCube cube) throws SaikuServiceException {
		try {
			return metaExplorer.getAllHierarchies(cube);
		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot get all hierarchies for cube ( " + cube + " )", e);	
		}
	}
	
	public List<SaikuHierarchy> getAllDimensionHierarchies(SaikuCube cube, String dimensionName) {
		try {
			SaikuDimension dim = metaExplorer.getDimension(cube, dimensionName);
			if (dim == null) {
				throw new SaikuServiceException("Cannot find dimension ( "+ dimensionName + ") for cube ( " + cube + " )");
			}
			return dim.getHierarchies();

		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot get all hierarchies for cube ( " + cube + " ) dimension ( " + dimensionName + " )", e);
		}
	}

	public List<SaikuLevel> getAllHierarchyLevels(SaikuCube cube, String dimensionName, String hierarchyName) {
		try {
			return  metaExplorer.getAllLevels(cube, dimensionName, hierarchyName);
		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot get all levels for cube ( " + cube 
					+ " ) dimension ( " + dimensionName + " ) hierarchy ( " + hierarchyName + " )", e);

		}
	}

	public List<SaikuMember> getLevelMembers(SaikuCube cube, String dimensionName, String hierarchyName, String levelName) {
		try {
			return  metaExplorer.getAllMembers(cube, dimensionName, hierarchyName, levelName);
		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot get all members for cube ( " + cube 
					+ " ) dimension ( " + dimensionName + " ) hierarchy ( " + hierarchyName + " )", e);
		}
	}
	
	public List<SaikuMember> getMeasures(SaikuCube cube) {
		try {
			return  metaExplorer.getAllMeasures(cube);
		} catch (SaikuOlapException e) {
			throw new SaikuServiceException("Cannot get all measures for cube ( " + cube + " )", e);
		}
	}
	
}
