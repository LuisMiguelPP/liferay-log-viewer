/**
 * Copyright (C) 2013 Permeance Technologies
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */

package com.cara.utils.logviewer.portlets;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.CharArrayWriter;
import java.io.Writer;

/**
 * LogHolder
 *
 */
public class LogHolder {

	public static final Log log = LogFactoryUtil.getLog(LogHolder.class);

	public static synchronized void attach(String name) throws Exception {
		if (!isAttached()) {
			try {
				final ClassLoader portalClassLoader =
					PortalClassLoaderUtil.getClassLoader();
				final Class<?> logger = portalClassLoader.loadClass(
					PortletConstants.LOG4J_LOGGER_CLASS);
				
				Object loggerObj = null;
				
				if(Validator.isNull(name))
					loggerObj = logger.getMethod(PortletConstants.GET_ROOT_LOGGER).invoke(null);
				else
					loggerObj = logger.getMethod(PortletConstants.GET_LOGGER, String.class).invoke(logger, name);
				
				final Class<?> patternLayout = portalClassLoader.loadClass(
					PortletConstants.LOG4J_PATTERN_LAYOUT_CLASS);

				final String pattern =
					PortletPropsValues.PERMEANCE_LOG_VIEWER_PATTERN;

				final Object patternLayoutObj = patternLayout.getConstructor(
					String.class).newInstance(pattern);

				final CharArrayWriter pwriter = new CharArrayWriter();
				viewer = new RollingLogViewer();
				runnable = new LogRunnable(pwriter, viewer);
				final Thread t = new Thread(runnable);
				t.start();

				final Class<?> writerAppender = portalClassLoader.loadClass(
					PortletConstants.LOG4J_WRITER_APPENDER_CLASS);

				final Class<?> appender = portalClassLoader.loadClass(
					PortletConstants.LOG4J_APPENDER_CLASS);
				final Class<?> layout = portalClassLoader.loadClass(
					PortletConstants.LOG4J_LAYOUT_CLASS);
				writerAppenderObj = writerAppender.getConstructor(
					layout, Writer.class).newInstance(patternLayoutObj,
					pwriter);

				logger.getMethod(
					PortletConstants.ADD_APPENDER,
					appender).invoke(loggerObj, writerAppenderObj);
				
				attached = true;
			} catch (final Exception e) {
				log.error(e);

				throw e;
			}
		}
	}

	public static synchronized void detach(String name) {
		if (isAttached()) {
			try {
				runnable.setStop(true);

				final ClassLoader portalClassLoader =
					PortalClassLoaderUtil.getClassLoader();
				final Class<?> logger = portalClassLoader.loadClass(
					PortletConstants.LOG4J_LOGGER_CLASS);
				Object loggerObj = null;
				if(Validator.isNull(name))
					loggerObj = logger.getMethod(PortletConstants.GET_ROOT_LOGGER).invoke(null);
				else
					loggerObj = logger.getMethod(PortletConstants.GET_LOGGER, String.class).invoke(logger, name);
				final Class<?> appender = portalClassLoader.loadClass(
					PortletConstants.LOG4J_APPENDER_CLASS);
				logger.getMethod(
					PortletConstants.REMOVE_APPENDER,
					appender).invoke(loggerObj, writerAppenderObj);
			} catch (final Exception e) {
				log.warn(e);
			}
		}

		runnable = null;
		viewer = null;
		writerAppenderObj = null;
		attached = false;
	}

	public static RollingLogViewer getViewer() {
		return viewer;
	}

	public static boolean isAttached() {
		return attached;
	}

	public static class LogRunnable implements Runnable {

		public LogRunnable(CharArrayWriter writer, RollingLogViewer viewer) {
			this.writer = writer;
			this.viewer = viewer;
		}

		public void run() {
			try {
				while (true) {
					final char[] buf = writer.toCharArray();
					writer.reset();

					if (buf.length > 0) {
						viewer.write(buf, 0, buf.length);
					}

					if (stop) {
						break;
					}

					try {
						Thread.sleep(
							PortletPropsValues.
								PERMEANCE_LOG_VIEWER_SLEEP_INTERVAL);
					} catch (final InterruptedException ie) {
					}
				}

			} catch (final Exception e) {
				log.warn(e);
			}
		}

		public void setStop(final boolean stop) {
			this.stop = stop;
		}

		private boolean stop = false;
		private final RollingLogViewer viewer;
		private final CharArrayWriter writer;

	}

	private static boolean attached = false;
	private static LogRunnable runnable = null;
	private static RollingLogViewer viewer = null;
	private static Object writerAppenderObj = null;

}