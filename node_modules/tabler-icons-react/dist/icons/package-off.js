import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function PackageOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-package-off",
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
    d: "M8.812 4.793l3.188 -1.793l8 4.5v8.5m-2.282 1.784l-5.718 3.216l-8 -4.5v-9l2.223 -1.25"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.543 10.57l5.457 -3.07"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 12v9"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 12l-8 -4.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 5.25l-4.35 2.447m-2.564 1.442l-1.086 .611"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { PackageOff as default };
