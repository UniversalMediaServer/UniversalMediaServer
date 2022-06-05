import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function SnowflakeOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-snowflake-off",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 4l2 1l2 -1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 2v6m1.196 1.186l1.804 1.034"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17.928 6.268l.134 2.232l1.866 1.232"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20.66 7l-5.629 3.25l-.031 .75"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19.928 14.268l-1.015 .67"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.212 14.226l-2.171 1.262"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 20l-2 -1l-2 1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 22v-6.5l-3 -1.72"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6.072 17.732l-.134 -2.232l-1.866 -1.232"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3.34 17l5.629 -3.25l-.01 -3.458"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.072 9.732l1.866 -1.232l.134 -2.232"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3.34 7l5.629 3.25l.802 -.466"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { SnowflakeOff as default };
